/*  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema for Android
 * Copyright (c) 2018-2024 Threema GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package ch.threema.app.activities.wizard;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import ch.threema.app.R;
import ch.threema.app.ThreemaApplication;
import ch.threema.app.dialogs.GenericProgressDialog;
import ch.threema.app.dialogs.PasswordEntryDialog;
import ch.threema.app.dialogs.SimpleStringAlertDialog;
import ch.threema.app.dialogs.WizardDialog;
import ch.threema.app.dialogs.WizardSafeSearchPhoneDialog;
import ch.threema.app.threemasafe.ThreemaSafeAdvancedDialog;
import ch.threema.app.threemasafe.ThreemaSafeMDMConfig;
import ch.threema.app.threemasafe.ThreemaSafeServerInfo;
import ch.threema.app.threemasafe.ThreemaSafeService;
import ch.threema.app.threemasafe.ThreemaSafeServiceImpl;
import ch.threema.app.ui.LongToast;
import ch.threema.app.utils.ConfigUtils;
import ch.threema.app.utils.DialogUtil;
import ch.threema.app.utils.RuntimeUtil;
import ch.threema.app.utils.TestUtil;
import ch.threema.app.utils.executor.BackgroundExecutor;
import ch.threema.app.utils.executor.BackgroundTask;
import ch.threema.app.workers.WorkSyncWorker;
import ch.threema.base.ThreemaException;
import ch.threema.base.utils.LoggingUtil;
import ch.threema.domain.protocol.csp.ProtocolDefines;

import static ch.threema.app.protocol.ApplicationSetupStepsKt.runApplicationSetupSteps;

public class WizardSafeRestoreActivity extends WizardBackgroundActivity implements PasswordEntryDialog.PasswordEntryDialogClickListener,
	WizardSafeSearchPhoneDialog.WizardSafeSearchPhoneDialogCallback,
	WizardDialog.WizardDialogCallback,
	ThreemaSafeAdvancedDialog.WizardDialogCallback {
	private static final Logger logger = LoggingUtil.getThreemaLogger("WizardSafeRestoreActivity");

	private static final String DIALOG_TAG_PASSWORD = "tpw";
	private static final String DIALOG_TAG_PROGRESS = "tpr";
	private static final String DIALOG_TAG_FORGOT_ID = "li";
	private static final String DIALOG_TAG_ADVANCED = "adv";
	private static final String DIALOG_TAG_WORK_SYNC = "workSync";
	private static final String DIALOG_TAG_PASSWORD_PRESET_CONFIRM = "safe_pw_preset";
	private static final String DIALOG_TAG_APPLICATION_SETUP_RETRY = "app-setup-retry";

	private ThreemaSafeService threemaSafeService;

	private final BackgroundExecutor executor = new BackgroundExecutor();

	EditText identityEditText;
	ThreemaSafeMDMConfig safeMDMConfig;
	ThreemaSafeServerInfo serverInfo = new ThreemaSafeServerInfo();

	private final ActivityResultLauncher<String> notificationPermissionLauncher =
		registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
			// Restore backup even if permission is not granted as we do not strictly require the
			// notification permission.
			doSafeRestore();
		});

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_wizard_restore_safe);

		try {
			threemaSafeService = ThreemaApplication.getServiceManager().getThreemaSafeService();
		} catch (Exception e) {
			finish();
			return;
		}

		this.safeMDMConfig = ThreemaSafeMDMConfig.getInstance();

		this.identityEditText = findViewById(R.id.safe_edit_id);

		if (ConfigUtils.isWorkRestricted()) {
			if (safeMDMConfig.isRestoreForced()) {
				this.identityEditText.setText(safeMDMConfig.getIdentity());
				this.identityEditText.setEnabled(false);

				findViewById(R.id.safe_restore_subtitle).setVisibility(View.INVISIBLE);
				findViewById(R.id.forgot_id).setVisibility(View.GONE);

				if (safeMDMConfig.isSkipRestorePasswordEntryDialog()) {
					reallySafeRestore(safeMDMConfig.getPassword());
				}
			}
		}

		this.identityEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
		this.identityEditText.setFilters(new InputFilter[]{new InputFilter.AllCaps(), new InputFilter.LengthFilter(ProtocolDefines.IDENTITY_LEN)});

		findViewById(R.id.forgot_id).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				WizardSafeSearchPhoneDialog.newInstance().show(getSupportFragmentManager(), DIALOG_TAG_FORGOT_ID);
			}
		});

		findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});

		findViewById(R.id.safe_restore_button).setOnClickListener(v -> {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
				// Ask for notification permission
				if (!ConfigUtils.requestNotificationPermission(WizardSafeRestoreActivity.this, notificationPermissionLauncher, preferenceService)) {
					return;
				}
			}

			if (identityEditText != null && identityEditText.getText() != null && identityEditText.getText().toString().length() == ProtocolDefines.IDENTITY_LEN) {
				doSafeRestore();
			} else {
				SimpleStringAlertDialog.newInstance(R.string.safe_restore, R.string.invalid_threema_id).show(getSupportFragmentManager(), "");
			}
		});

		Button advancedOptions = findViewById(R.id.advanced_options);
		advancedOptions.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				ThreemaSafeAdvancedDialog dialog = ThreemaSafeAdvancedDialog.newInstance(serverInfo, false);
				dialog.show(getSupportFragmentManager(), DIALOG_TAG_ADVANCED);
			}
		});

		if (ConfigUtils.isWorkRestricted()) {
			if (safeMDMConfig.isRestoreExpertSettingsDisabled()) {
				advancedOptions.setEnabled(false);
				advancedOptions.setVisibility(View.GONE);
			}
		}
	}

	@Override
	protected void onPause() {
		ThreemaApplication.activityPaused(this);
		super.onPause();
	}

	@Override
	protected void onResume() {
		ThreemaApplication.activityResumed(this);
		super.onResume();
	}

	@Override
	public void onUserInteraction() {
		ThreemaApplication.activityUserInteract(this);
		super.onUserInteraction();
	}

	private void doSafeRestore() {
		PasswordEntryDialog dialogFragment = PasswordEntryDialog.newInstance(
			R.string.safe_enter_password,
			R.string.restore_data_password_msg,
			R.string.password_hint,
			R.string.ok,
			R.string.cancel,
			ThreemaSafeServiceImpl.MIN_PW_LENGTH,
			ThreemaSafeServiceImpl.MAX_PW_LENGTH,
			0, 0, 0, PasswordEntryDialog.ForgotHintType.SAFE);
		dialogFragment.show(getSupportFragmentManager(), DIALOG_TAG_PASSWORD);
	}

	@SuppressLint("StaticFieldLeak")
	private void reallySafeRestore(String password) {
		final String identity;

		if (safeMDMConfig.isRestoreForced()) {
			serverInfo = safeMDMConfig.getServerInfo();
			identity = safeMDMConfig.getIdentity();
		} else {
			if (safeMDMConfig.isRestoreExpertSettingsDisabled()) {
				serverInfo = safeMDMConfig.getServerInfo();
			}
			if (identityEditText.getText() != null) {
				identity = identityEditText.getText().toString();
			} else {
				identity = null;
			}
		}

		if (TestUtil.empty(identity)) {
			Toast.makeText(this, R.string.invalid_threema_id, Toast.LENGTH_LONG).show();
			return;
		}

		if (TestUtil.empty(password)) {
			LongToast.makeText(this, R.string.wrong_backupid_or_password_or_no_internet_connection, Toast.LENGTH_LONG).show();
			return;
		}

		preferenceService.setLatestVersion(this);

		new AsyncTask<Void, Void, String>() {
			@Override
			protected void onPreExecute() {
				GenericProgressDialog.newInstance(R.string.restore, R.string.please_wait).show(getSupportFragmentManager(), DIALOG_TAG_PROGRESS);
				try {
					serviceManager.stopConnection();
				} catch (InterruptedException e) {
					this.cancel(true);
				}
			}

			@Override
			protected String doInBackground(Void... voids) {
				if (this.isCancelled()) {
					return "Backup cancelled";
				}
				preferenceService.setThreemaSafeEnabled(false);
				try {
					threemaSafeService.restoreBackup(identity, password, serverInfo);
					threemaSafeService.testServer(serverInfo);  // intentional: test server to update configuration only after restoring backup, so that master key (and thus shard hash) is set
					threemaSafeService.setEnabled(true);
					return null;
				} catch (ThreemaException e) {
					return e.getMessage();
				} catch (IOException e) {
					if (e instanceof FileNotFoundException) {
						return getString(R.string.safe_no_backup_found);
					}
					return e.getLocalizedMessage();
				}
			}

			@Override
			protected void onCancelled(String failureMessage) {
				this.onPostExecute(failureMessage);
			}

			@Override
			protected void onPostExecute(String failureMessage) {
				DialogUtil.dismissDialog(getSupportFragmentManager(), DIALOG_TAG_PROGRESS, true);

				if (failureMessage == null) {
					runApplicationSetupStepsAndFinish();
				} else {
					LongToast.makeText(WizardSafeRestoreActivity.this, getString(R.string.safe_restore_failed) + ". " + failureMessage, Toast.LENGTH_LONG).show();
					if (safeMDMConfig.isRestoreForced()) {
						finish();
					}
				}
			}
		}.execute();
	}

	private void runApplicationSetupStepsAndFinish() {
		executor.execute(new BackgroundTask<Boolean>() {
			@Override
			public void runBefore() {
				// Nothing to do
			}

			@Override
			public Boolean runInBackground() {
				return runApplicationSetupSteps(serviceManager, WizardSafeRestoreActivity.this);
			}

			@Override
			public void runAfter(Boolean result) {
				if (Boolean.TRUE.equals(result)) {
					finishSuccessfully();
				} else {
					WizardDialog.newInstance(R.string.application_setup_steps_failed, R.string.retry)
						.show(getSupportFragmentManager(), DIALOG_TAG_APPLICATION_SETUP_RETRY);
				}
			}
		});
	}

	private void finishSuccessfully() {
		if (ConfigUtils.isWorkBuild()) {
			GenericProgressDialog.newInstance(R.string.work_data_sync_desc,
				R.string.please_wait).show(getSupportFragmentManager(), DIALOG_TAG_WORK_SYNC);

			WorkSyncWorker.Companion.performOneTimeWorkSync(
				WizardSafeRestoreActivity.this,
				() -> {
					// On success
					DialogUtil.dismissDialog(getSupportFragmentManager(), DIALOG_TAG_WORK_SYNC, true);
					onSuccessfulRestore();
				},
				() -> {
					// On fail
					DialogUtil.dismissDialog(getSupportFragmentManager(), DIALOG_TAG_WORK_SYNC, true);
					RuntimeUtil.runOnUiThread(() -> Toast.makeText(WizardSafeRestoreActivity.this, R.string.unable_to_fetch_configuration, Toast.LENGTH_LONG).show());
					logger.warn("Unable to post work request for fetch2 or preset password was denied");
					removeIdentity();
				});
		} else {
			onSuccessfulRestore();
		}
	}

	private void removeIdentity() {
		try {
			userService.removeIdentity();
		} catch (Exception e) {
			logger.error("Unable to remove identity", e);
		}
		finishAndRemoveTask();
	}

	private void onSuccessfulRestore() {
		if (safeMDMConfig.isBackupPasswordPreset()) {
			WizardDialog wizardDialog = WizardDialog.newInstance(R.string.safe_managed_password_confirm, R.string.accept, R.string.real_not_now, WizardDialog.Highlight.NONE);
			wizardDialog.show(getSupportFragmentManager(), DIALOG_TAG_PASSWORD_PRESET_CONFIRM);
		} else {
			scheduleAppRestart();
		}
	}

	private void scheduleAppRestart() {
		SimpleStringAlertDialog.newInstance(R.string.restore_success_body, R.string.android_backup_restart_threema,
			true).show(getSupportFragmentManager(), "d");
		try {
			serviceManager.startConnection();
		} catch (ThreemaException e) {
			logger.error("Exception", e);
		}
		ConfigUtils.scheduleAppRestart(getApplicationContext(), 3000, null);
	}

	@Override
	protected boolean enableOnBackPressedCallback() {
		// Override the behavior of WizardBackgroundActivity to allow normal back navigation
		return false;
	}

	@Override
	public void onYes(String tag, String text, boolean isChecked, Object data) {
		// safe backup restore
		if (!TestUtil.empty(text)) {
			reallySafeRestore(text);
		}
	}

	@Override
	public void onYes(String tag, String id) {
		// return from id search
		if (id != null && id.length() == ProtocolDefines.IDENTITY_LEN) {
			this.identityEditText.setText("");
			this.identityEditText.append(id);
		}
	}

	@Override
	public void onYes(String tag, ThreemaSafeServerInfo serverInfo) {
		this.serverInfo = serverInfo;
	}

	@Override
	public void onYes(String tag, Object data) {
		if (DIALOG_TAG_PASSWORD_PRESET_CONFIRM.equals(tag)) {
			scheduleAppRestart();
		} else if (DIALOG_TAG_APPLICATION_SETUP_RETRY.equals(tag)) {
			runApplicationSetupStepsAndFinish();
		}
	}

	@Override
	public void onNo(String tag) {
		if (DIALOG_TAG_PASSWORD_PRESET_CONFIRM.equals(tag)) {
			removeIdentity();
		} else if (safeMDMConfig.isRestoreDisabled()) {
			finish();
		}
	}
}
