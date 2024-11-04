/*  _____ _
 * |_   _| |_  _ _ ___ ___ _ __  __ _
 *   | | | ' \| '_/ -_) -_) '  \/ _` |_
 *   |_| |_||_|_| \___\___|_|_|_\__,_(_)
 *
 * Threema for Android
 * Copyright (c) 2013-2024 Threema GmbH
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

package ch.threema.app.activities;

import static ch.threema.app.services.ConversationTagServiceImpl.FIXED_TAG_UNREAD;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.format.DateUtils;
import android.text.style.TextAppearanceSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.AnyThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.lifecycle.LifecycleOwner;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.badge.BadgeDrawable;
import com.google.android.material.badge.ExperimentalBadgeUtils;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.shape.MaterialShapeDrawable;

import org.slf4j.Logger;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

import ch.threema.app.BuildFlavor;
import ch.threema.app.R;
import ch.threema.app.ThreemaApplication;
import ch.threema.app.activities.wizard.WizardBaseActivity;
import ch.threema.app.activities.wizard.WizardStartActivity;
import ch.threema.app.archive.ArchiveActivity;
import ch.threema.app.backuprestore.csv.BackupService;
import ch.threema.app.backuprestore.csv.RestoreService;
import ch.threema.app.dialogs.GenericAlertDialog;
import ch.threema.app.dialogs.GenericProgressDialog;
import ch.threema.app.dialogs.SMSVerificationDialog;
import ch.threema.app.dialogs.ShowOnceDialog;
import ch.threema.app.dialogs.SimpleStringAlertDialog;
import ch.threema.app.exceptions.EntryAlreadyExistsException;
import ch.threema.app.exceptions.FileSystemNotPresentException;
import ch.threema.app.fragments.ContactsSectionFragment;
import ch.threema.app.fragments.MessageSectionFragment;
import ch.threema.app.fragments.MyIDFragment;
import ch.threema.app.glide.AvatarOptions;
import ch.threema.app.globalsearch.GlobalSearchActivity;
import ch.threema.app.grouplinks.OutgoingGroupRequestActivity;
import ch.threema.app.listeners.AppIconListener;
import ch.threema.app.listeners.ContactCountListener;
import ch.threema.app.listeners.ConversationListener;
import ch.threema.app.listeners.MessageListener;
import ch.threema.app.listeners.ProfileListener;
import ch.threema.app.listeners.SMSVerificationListener;
import ch.threema.app.listeners.VoipCallListener;
import ch.threema.app.managers.ListenerManager;
import ch.threema.app.managers.ServiceManager;
import ch.threema.app.messagereceiver.MessageReceiver;
import ch.threema.app.multidevice.LinkedDevicesActivity;
import ch.threema.app.preference.SettingsActivity;
import ch.threema.app.push.PushService;
import ch.threema.app.qrscanner.activity.BaseQrScannerActivity;
import ch.threema.app.routines.CheckLicenseRoutine;
import ch.threema.app.services.ContactService;
import ch.threema.app.services.ConversationService;
import ch.threema.app.services.ConversationTagService;
import ch.threema.app.services.DeviceService;
import ch.threema.app.services.FileService;
import ch.threema.app.services.LockAppService;
import ch.threema.app.services.MessageService;
import ch.threema.app.services.NotificationService;
import ch.threema.app.services.PassphraseService;
import ch.threema.app.services.PreferenceService;
import ch.threema.app.services.ThreemaPushService;
import ch.threema.app.services.UpdateSystemService;
import ch.threema.app.services.UserService;
import ch.threema.app.services.license.LicenseService;
import ch.threema.app.tasks.ApplicationUpdateStepsTask;
import ch.threema.app.threemasafe.ThreemaSafeMDMConfig;
import ch.threema.app.threemasafe.ThreemaSafeService;
import ch.threema.app.ui.IdentityPopup;
import ch.threema.app.ui.OngoingCallNoticeMode;
import ch.threema.app.ui.OngoingCallNoticeView;
import ch.threema.app.utils.AnimationUtil;
import ch.threema.app.utils.AppRestrictionUtil;
import ch.threema.app.utils.BitmapUtil;
import ch.threema.app.utils.ConfigUtils;
import ch.threema.app.utils.ConnectionIndicatorUtil;
import ch.threema.app.utils.DialogUtil;
import ch.threema.app.utils.IntentDataUtil;
import ch.threema.app.utils.PowermanagerUtil;
import ch.threema.app.utils.RuntimeUtil;
import ch.threema.app.utils.TestUtil;
import ch.threema.app.voip.groupcall.GroupCallDescription;
import ch.threema.app.voip.groupcall.GroupCallManager;
import ch.threema.app.voip.groupcall.GroupCallObserver;
import ch.threema.app.voip.groupcall.sfu.GroupCallController;
import ch.threema.app.voip.services.VoipCallService;
import ch.threema.app.webclient.activities.SessionsActivity;
import ch.threema.base.utils.LoggingUtil;
import ch.threema.domain.protocol.api.LinkMobileNoException;
import ch.threema.domain.protocol.connection.ServerConnection;
import ch.threema.domain.protocol.connection.ConnectionState;
import ch.threema.domain.protocol.connection.ConnectionStateListener;
import ch.threema.localcrypto.MasterKey;
import ch.threema.storage.DatabaseServiceNew;
import ch.threema.storage.models.AbstractMessageModel;
import ch.threema.storage.models.ContactModel;
import ch.threema.storage.models.ConversationModel;
import ch.threema.storage.models.MessageState;
import ch.threema.storage.models.TagModel;

public class HomeActivity extends ThreemaAppCompatActivity implements
	SMSVerificationDialog.SMSVerificationDialogCallback,
	GenericAlertDialog.DialogClickListener,
	LifecycleOwner {

	private static final Logger logger = LoggingUtil.getThreemaLogger("HomeActivity");

	public static final String THREEMA_CHANNEL_IDENTITY = "*THREEMA";
	private static final String THREEMA_CHANNEL_INFO_COMMAND = "Info";
	private static final String THREEMA_CHANNEL_START_NEWS_COMMAND = "Start News";
	private static final String THREEMA_CHANNEL_START_ANDROID_COMMAND = "Start Android";
	private static final String THREEMA_CHANNEL_WORK_COMMAND = "Start Threema Work";

	private static final long PHONE_REQUEST_DELAY = 10 * DateUtils.MINUTE_IN_MILLIS;

	private static final String DIALOG_TAG_VERIFY_CODE = "vc";
	private static final String DIALOG_TAG_VERIFY_CODE_CONFIRM = "vcc";
	private static final String DIALOG_TAG_CANCEL_VERIFY = "cv";
	private static final String DIALOG_TAG_MASTERKEY_LOCKED = "mkl";
	private static final String DIALOG_TAG_SERIAL_LOCKED = "sll";
	private static final String DIALOG_TAG_FINISH_UP = "fup";
	private static final String DIALOG_TAG_THREEMA_CHANNEL_VERIFY = "cvf";
	private static final String DIALOG_TAG_UPDATING = "updating";
	private static final String DIALOG_TAG_PASSWORD_PRESET_CONFIRM = "pwconf";

	private static final String FRAGMENT_TAG_MESSAGES = "0";
	private static final String FRAGMENT_TAG_CONTACTS = "1";
	private static final String FRAGMENT_TAG_PROFILE = "2";

	private static final String BUNDLE_CURRENT_FRAGMENT_TAG = "currentFragmentTag";
	private static final int REQUEST_CODE_WHATSNEW = 41912;

	public static final String EXTRA_SHOW_CONTACTS = "show_contacts";

	private ActionBar actionBar;
	private boolean isLicenseCheckStarted = false, isInitialized = false, isWhatsNewShown = false, isUpdating = false;
	private MaterialToolbar toolbar;
	private View connectionIndicator;
	private View noticeSMSLayout;
	private OngoingCallNoticeView ongoingCallNotice;
	private static long starredMessagesCount = 0L;

	private ServiceManager serviceManager;
	private NotificationService notificationService;
	private UserService userService;
	private ContactService contactService;
	private LockAppService lockAppService;
	private PreferenceService preferenceService;
	private ConversationService conversationService;
	private GroupCallManager groupCallManager;

	private enum UnsentMessageAction {
		ADD,
		REMOVE,
	}

	private final List<AbstractMessageModel> unsentMessages = new LinkedList<>();

	private final ActivityResultLauncher<String> notificationPermissionLauncher =
		registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
			if (!Boolean.TRUE.equals(isGranted)) {
				logger.warn("Notification permission was not granted");
			}
		});

	private final ActivityResultLauncher<Intent> problemSolverLauncher =
		registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> updateWarningButton());

	private BroadcastReceiver checkLicenseBroadcastReceiver = null;
	private final BroadcastReceiver currentCheckAppReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, final Intent intent) {
			RuntimeUtil.runOnUiThread(() -> {
				if (IntentDataUtil.ACTION_LICENSE_NOT_ALLOWED.equals(intent.getAction())) {
					if (BuildFlavor.getLicenseType() == BuildFlavor.LicenseType.SERIAL ||
						BuildFlavor.getLicenseType() == BuildFlavor.LicenseType.GOOGLE_WORK ||
						BuildFlavor.getLicenseType() == BuildFlavor.LicenseType.HMS_WORK ||
						BuildFlavor.getLicenseType() == BuildFlavor.LicenseType.ONPREM) {
						//show enter serial stuff
						startActivityForResult(new Intent(HomeActivity.this, EnterSerialActivity.class), ThreemaActivity.ACTIVITY_ID_ENTER_SERIAL);
					} else {
						showErrorTextAndExit(IntentDataUtil.getMessage(intent));
					}
				} else if (IntentDataUtil.ACTION_UPDATE_AVAILABLE.equals(intent.getAction()) && BuildFlavor.maySelfUpdate() && userService != null && userService.hasIdentity()) {
					logger.info("App update available. Opening DownloadApkActivity.");
					new Handler().postDelayed(() -> {
						Intent dialogIntent = new Intent(intent);
						dialogIntent.setClass(HomeActivity.this, DownloadApkActivity.class);
						startActivity(dialogIntent);
					}, DateUtils.SECOND_IN_MILLIS * 5);
				}
			});
		}
	};

	private BottomNavigationView bottomNavigationView;
	private View mainContent, toolbarWarningButton;

	private String currentFragmentTag;

	private static class UpdateBottomNavigationBadgeTask extends AsyncTask<Void, Void, Integer> {
		private ConversationTagService conversationTagService = null;
		private final WeakReference<Activity> activityWeakReference;

		UpdateBottomNavigationBadgeTask(Activity activity) {
			activityWeakReference = new WeakReference<>(activity);
			try {
				conversationTagService = Objects.requireNonNull(ThreemaApplication.getServiceManager()).getConversationTagService();
			} catch (Exception e) {
				logger.error("UpdateBottomNav", e);
			}
		}

		@Override
		protected Integer doInBackground(Void... voids) {
			ConversationService conversationService;
			try {
				conversationService = ThreemaApplication.getServiceManager().getConversationService();
			} catch (Exception e) {
				return 0;
			}

			if (conversationService == null) {
				return 0;
			}

			List <ConversationModel> conversationModels = conversationService.getAll(false, new ConversationService.Filter() {
				@Override
				public boolean onlyUnread() {
					return true;
				}

				@Override
				public boolean noDistributionLists() {
					return false;
				}

				@Override
				public boolean noHiddenChats() {
					return false;
				}

				@Override
				public boolean noInvalid() {
					return false;
				}
			});

			int unread = 0;

			for(ConversationModel conversationModel : conversationModels) {
				unread += conversationModel.getUnreadCount();
			}

			if (conversationTagService != null) {
				TagModel unreadTagModel = conversationTagService.getTagModel(FIXED_TAG_UNREAD);
				if (unreadTagModel == null) {
					logger.error("Unread tag model is null");
					return unread;
				}

				// First check whether there are some conversations that are marked as unread. This
				// check is expected to be fast, as usually there are not many chats that are marked
				// as unread.
				if (conversationTagService.getCount(unreadTagModel) > 0) {
					// In case there is at least one unread tag, we create a set of all possible
					// conversation uids to efficiently check that the unread tags are valid.
					Set<String> shownConversationUids = conversationService.getAll(false)
						.stream()
						.map(ConversationModel::getUid)
						.collect(Collectors.toSet());

					List<String> unreadUids = conversationTagService.getConversationUidsByTag(unreadTagModel);
					for (String unreadUid : unreadUids) {
						if (shownConversationUids.contains(unreadUid)) {
							unread++;
						} else {
							logger.warn("Conversation '{}' is marked as unread but not shown. Deleting the unread flag.", unreadUid);
							conversationTagService.removeTag(unreadUid, unreadTagModel);
						}
					}
				}
			}

			return unread;
		}

		@Override
		protected void onPostExecute(Integer count) {
			if (activityWeakReference.get() != null) {
				BottomNavigationView bottomNavigationView = activityWeakReference.get().findViewById(R.id.bottom_navigation);
				if (bottomNavigationView != null) {
					BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.messages);
					if (badgeDrawable.getVerticalOffset() == 0) {
						badgeDrawable.setVerticalOffset(activityWeakReference.get().getResources().getDimensionPixelSize(R.dimen.bottom_nav_badge_offset_vertical));
					}
					badgeDrawable.setNumber(count);
					badgeDrawable.setVisible(count > 0);
				}
			}
		}
	}

	private static class UpdateStarredMessagesTask extends AsyncTask<Void, Void, Long> {
		@Override
		protected Long doInBackground(Void... voids) {
			MessageService messageService;
			try {
				ServiceManager serviceManager = ThreemaApplication.getServiceManager();
				if (serviceManager != null) {
					messageService = serviceManager.getMessageService();
					return messageService.countStarredMessages();
				} else {
					if (logger != null) {
						logger.warn("Could not count starred messages because service manager is null");
					}
					return 0L;
				}
			} catch (Exception e) {
				if (logger != null) {
					logger.error("Unable to count starred messages", e);
				}
				return 0L;
			}
		}

		@Override
		protected void onPostExecute(Long count) {
			starredMessagesCount = count;
		}
	}

	private final ConnectionStateListener connectionStateListener = this::updateConnectionIndicator;

	private void updateUnsentMessagesList(AbstractMessageModel modifiedMessageModel, UnsentMessageAction action) {
		int numCurrentUnsent = unsentMessages.size();

		synchronized (unsentMessages) {
			String uid = modifiedMessageModel.getUid();

			// Check whether the message model with the same uid is already in the list or not
			AbstractMessageModel containedMessageModel = null;
			for (AbstractMessageModel unsentMessage : unsentMessages) {
				if (TestUtil.compare(unsentMessage.getUid(), uid)) {
					containedMessageModel = unsentMessage;
					break;
				}
			}

			switch (action) {
				case ADD:
					// Only add the message model if it is not yet in the list
					if (containedMessageModel == null) {
						unsentMessages.add(modifiedMessageModel);
					}
					break;
				case REMOVE:
					// Remove message model if it is in the list
					if (containedMessageModel != null) {
						unsentMessages.remove(containedMessageModel);
					}
					break;
			}

			int numNewUnsent = unsentMessages.size();

			// Update the notification if there was a change
			if (notificationService != null && numCurrentUnsent != numNewUnsent) {
				notificationService.showUnsentMessageNotification(unsentMessages);
			}
		}
	}

	private final SMSVerificationListener smsVerificationListener = new SMSVerificationListener() {
		@Override
		public void onVerified() {
			RuntimeUtil.runOnUiThread(() -> {
				if (noticeSMSLayout != null) {
					AnimationUtil.collapse(noticeSMSLayout, null, true);
				}
			});
		}

		@Override
		public void onVerificationStarted() {
			RuntimeUtil.runOnUiThread(() -> {
				if (noticeSMSLayout != null) {
					AnimationUtil.expand(noticeSMSLayout, null, true);
				}
			});
		}
	};

	private void updateBottomNavigation() {
		if (preferenceService.getShowUnreadBadge()) {
			RuntimeUtil.runOnUiThread(() -> {
				try {
					new UpdateBottomNavigationBadgeTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} catch (RejectedExecutionException e) {
					try {
						new UpdateBottomNavigationBadgeTask(this).executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
					} catch (RejectedExecutionException ignored) {
					}
				}
			});
		}
	}

	private final ConversationListener conversationListener = new ConversationListener() {
		@Override
		public void onNew(ConversationModel conversationModel) {
			updateBottomNavigation();
		}

		@Override
		public void onModified(ConversationModel modifiedConversationModel, Integer oldPosition) {
			updateBottomNavigation();
		}

		@Override
		public void onRemoved(ConversationModel conversationModel) {
			updateBottomNavigation();
		}

		@Override
		public void onModifiedAll() {
			updateBottomNavigation();
		}
	};

	private final MessageListener messageListener = new MessageListener() {
		@Override
		public void onNew(AbstractMessageModel newMessage) {
			//do nothing
		}

		@Override
		public void onModified(List<AbstractMessageModel> modifiedMessageModels) {
			for (AbstractMessageModel modifiedMessageModel : modifiedMessageModels) {

				if (!modifiedMessageModel.isStatusMessage()
					&& modifiedMessageModel.isOutbox()) {

					if (modifiedMessageModel.getState() == MessageState.SENDFAILED) {
						updateUnsentMessagesList(modifiedMessageModel, UnsentMessageAction.ADD);
					} else {
						updateUnsentMessagesList(modifiedMessageModel, UnsentMessageAction.REMOVE);
					}
				}
			}
		}

		@Override
		public void onRemoved(AbstractMessageModel removedMessageModel) {
			updateUnsentMessagesList(removedMessageModel, UnsentMessageAction.REMOVE);
		}

		@Override
		public void onRemoved(List<AbstractMessageModel> removedMessageModels) {
			for (AbstractMessageModel removedMessageModel: removedMessageModels) {
				updateUnsentMessagesList(removedMessageModel, UnsentMessageAction.REMOVE);
			}
		}

		@Override
		public void onProgressChanged(AbstractMessageModel messageModel, int newProgress) {
			//do nothing
		}

		@Override
		public void onResendDismissed(@NonNull AbstractMessageModel messageModel) {
			updateUnsentMessagesList(messageModel, UnsentMessageAction.REMOVE);
		}
	};

	private final AppIconListener appIconListener = () -> RuntimeUtil.runOnUiThread(this::updateAppLogo);

	private final ProfileListener profileListener = new ProfileListener() {
		@Override
		public void onAvatarChanged() {
			RuntimeUtil.runOnUiThread(() -> updateDrawerImage());
		}

		@Override
		public void onAvatarRemoved() {
			this.onAvatarChanged();
		}

		@Override
		public void onNicknameChanged(String newNickname) { }
	};

	private final VoipCallListener voipCallListener = new VoipCallListener() {
		@Override
		public void onStart(String contact, long elpasedTimeMs) {
			updateOngoingCallNotice();
		}

		@Override
		public void onEnd() {
			hideOngoingVoipCallNotice();
		}
	};

	private final GroupCallObserver groupCallObserver = call -> updateOngoingCallNotice();

	private final ContactCountListener contactCountListener = new ContactCountListener() {
		@Override
		public void onNewContactsCountUpdated(int last24hoursCount) {
			if (preferenceService != null && preferenceService.getShowUnreadBadge()) {
				RuntimeUtil.runOnUiThread(() -> {
					if (!isFinishing() && !isDestroyed() && !isChangingConfigurations()) {
						BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
						if (bottomNavigationView != null) {
							BadgeDrawable badgeDrawable = bottomNavigationView.getOrCreateBadge(R.id.contacts);
							badgeDrawable.setBackgroundColor(ConfigUtils.getAccentColor(HomeActivity.this));
							if (badgeDrawable.getVerticalOffset() == 0) {
								badgeDrawable.setVerticalOffset(getResources().getDimensionPixelSize(R.dimen.bottom_nav_badge_offset_vertical));
							}
							badgeDrawable.setVisible(last24hoursCount > 0);
						}
					}
				});
			}
		}
	};

	@Override
	@ExperimentalBadgeUtils
	protected void onCreate(Bundle savedInstanceState) {
		logger.info("onCreate");

		final boolean isColdStart = savedInstanceState == null;

		ConfigUtils.configureSystemBars(this);

		super.onCreate(savedInstanceState);

		if (BackupService.isRunning() || RestoreService.isRunning()) {
			startActivity(new Intent(this, BackupRestoreProgressActivity.class));
			finish();
			return;
		}

		//check master key
		MasterKey masterKey = ThreemaApplication.getMasterKey();

		if (masterKey != null && masterKey.isLocked()) {
			startActivityForResult(new Intent(this, UnlockMasterKeyActivity.class), ThreemaActivity.ACTIVITY_ID_UNLOCK_MASTER_KEY);
		} else {
			if (ConfigUtils.isSerialLicensed() && !ConfigUtils.isSerialLicenseValid()) {
				boolean isInstalledFromStore = ConfigUtils.isInstalledFromStore(ThreemaApplication.getAppContext());
				if (ConfigUtils.isWorkBuild() && !ConfigUtils.isWorkRestricted() && !hasIdentity() && isInstalledFromStore) {
					startActivityForResult(new Intent(this, WorkIntroActivity.class), ThreemaActivity.ACTIVITY_ID_WORK_INTRO);
				} else {
					startActivityForResult(new Intent(this, EnterSerialActivity.class), ThreemaActivity.ACTIVITY_ID_ENTER_SERIAL);
				}
				finish();
			} else {
				this.startMainActivity(savedInstanceState);

				// only execute this on first startup
				if (isColdStart) {
					if (preferenceService != null && userService != null && userService.hasIdentity()) {
						if (ConfigUtils.isWorkRestricted()) {
							// update configuration
							final ThreemaSafeMDMConfig newConfig = ThreemaSafeMDMConfig.getInstance();
							ThreemaSafeService threemaSafeService = getThreemaSafeService();

							if (threemaSafeService != null) {
								if (newConfig.hasChanged(preferenceService)) {
									if (newConfig.isBackupForced()) {
										if (newConfig.isSkipBackupPasswordEntry()) {
											// enable with given password
											byte[] newMasterKey = threemaSafeService.deriveMasterKey(newConfig.getPassword(), newConfig.getIdentity());
											byte[] oldMasterKey = preferenceService.getThreemaSafeMasterKey();

											// show warning dialog only when password was changed
											if (Arrays.equals(newMasterKey, oldMasterKey)) {
												reconfigureSafe(threemaSafeService, newConfig);
												enableSafe(threemaSafeService, newConfig, null);
											} else {
												GenericAlertDialog dialog = GenericAlertDialog.newInstance(R.string.threema_safe, R.string.safe_managed_new_password_confirm, R.string.accept, R.string.real_not_now);
												dialog.setData(newConfig);
												dialog.show(getSupportFragmentManager(), DIALOG_TAG_PASSWORD_PRESET_CONFIRM);
											}
										} else if (threemaSafeService.getThreemaSafeMasterKey() != null && threemaSafeService.getThreemaSafeMasterKey().length > 0) {
											// no password has been given by admin but a master key from a previous backup exists
											// -> create a new backup with existing password
											reconfigureSafe(threemaSafeService, newConfig);
											enableSafe(threemaSafeService, newConfig, threemaSafeService.getThreemaSafeMasterKey());
										} else {
											reconfigureSafe(threemaSafeService, newConfig);
											threemaSafeService.launchForcedPasswordDialog(this, true);
											finish();
											return;
										}
									} else {
										reconfigureSafe(threemaSafeService, newConfig);
									}
								} else {
									if (newConfig.isBackupForced() && !preferenceService.getThreemaSafeEnabled()) {
										// config has not changed but safe is still not enabled. fix it.
										if (newConfig.isSkipBackupPasswordEntry()) {
											// enable with given password
											enableSafe(threemaSafeService, newConfig, null);
										} else {
											// ask user for a new password
											threemaSafeService.launchForcedPasswordDialog(this, true);
											finish();
											return;
										}
									}
								}
								// save current config as new reference
								newConfig.saveConfig(preferenceService);
							}
						}
					}
				}
			}
		}
	}

	private boolean hasIdentity() {
		try {
			return ThreemaApplication.requireServiceManager().getUserService().hasIdentity();
		} catch (NullPointerException npe) {
			logger.error("user service not available");
			return false;
		}
	}

	private void reconfigureSafe(@NonNull ThreemaSafeService threemaSafeService, @NonNull ThreemaSafeMDMConfig newConfig) {
		// dispose of old backup, if any
		try {
			threemaSafeService.deleteBackup();
			threemaSafeService.setEnabled(false);
		} catch (Exception e) {
			// ignore
		}

		if (preferenceService != null) {
			preferenceService.setThreemaSafeServerInfo(newConfig.getServerInfo());
		}
	}

	private ThreemaSafeService getThreemaSafeService() {
		try {
			return serviceManager.getThreemaSafeService();
		} catch (Exception e) {
			//
		}
		return null;
	}

	@Override
	protected void onStart() {
		super.onStart();

		// Check if there are any server messages to display
		if (serviceManager != null) {
			DatabaseServiceNew databaseService = serviceManager.getDatabaseServiceNew();
			try {
				if (databaseService.getServerMessageModelFactory().count() > 0) {
					Intent intent = new Intent(this, ServerMessageActivity.class);
					startActivity(intent);
				}
			} catch (SQLiteException e) {
				logger.error("Could not get server message model count", e);
			}
		}
	}

	@UiThread
	private void showMainContent() {
		if (mainContent != null) {
			if (mainContent.getVisibility() != View.VISIBLE) {
				mainContent.setVisibility(View.VISIBLE);
			}
		}
	}

	private void updateWarningButton() {
		logger.debug("updateWarningButton");
		if (toolbarWarningButton != null) {
			toolbarWarningButton.setVisibility(shouldShowToolbarWarning() ? View.VISIBLE : View.GONE);
		}
	}

	private boolean shouldShowToolbarWarning() {
		return false ||
			ConfigUtils.isBackgroundRestricted(ThreemaApplication.getAppContext()) ||
			ConfigUtils.isBackgroundDataRestricted(ThreemaApplication.getAppContext(), false) ||
			ConfigUtils.isNotificationsDisabled(ThreemaApplication.getAppContext()) ||
			(preferenceService.isCallsEnabled() && ConfigUtils.isFullScreenNotificationsDisabled(ThreemaApplication.getAppContext())) ||
			((preferenceService.useThreemaPush() || BuildFlavor.forceThreemaPush()) && !PowermanagerUtil.isIgnoringBatteryOptimizations(ThreemaApplication.getAppContext()));
	}

	private void showWhatsNew() {
		final boolean skipWhatsNew = true; // set this to false if you want to show a What's New screen

		if (preferenceService != null) {
			if (!preferenceService.isLatestVersion(this)) {
				// so the app has just been updated
				ConfigUtils.requestNotificationPermission(this, notificationPermissionLauncher, preferenceService);

				if (preferenceService.getPrivacyPolicyAccepted() == null) {
					preferenceService.setPrivacyPolicyAccepted(new Date(), PreferenceService.PRIVACY_POLICY_ACCEPT_UPDATE);
				}

				if (!ConfigUtils.isWorkBuild() && !RuntimeUtil.isInTest() && !isFinishing()) {
					if (skipWhatsNew) {
						isWhatsNewShown = false; // make sure isWhatsNewShown is set to false here if whatsnew is skipped - otherwise pin unlock will not be shown once
					} else {
						int previous = preferenceService.getLatestVersion() % 1000;

						// To not show the same dialog twice, it is only shown if the previous version
						// is prior to the first version that used this dialog.
						// Use the version code of the first version where this dialog should be shown.
						if (previous < 925) { // do not show to users of previous release candidate
							isWhatsNewShown = true;

							Intent intent = new Intent(this, WhatsNewActivity.class);
							startActivityForResult(intent, REQUEST_CODE_WHATSNEW);
							overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out);
						}
					}
					preferenceService.setLatestVersion(this);
				}
			}
		}
	}

	@SuppressLint("StaticFieldLeak")
	private void enableSafe(@NonNull ThreemaSafeService threemaSafeService, ThreemaSafeMDMConfig mdmConfig, final byte[] masterkeyPreset) {
		new AsyncTask<Void, Void, byte[]>() {
			@Override
			protected byte[] doInBackground(Void... voids) {
				if (masterkeyPreset == null) {
					return threemaSafeService.deriveMasterKey(mdmConfig.getPassword(), userService.getIdentity());
				}
				return masterkeyPreset;
			}

			@Override
			protected void onPostExecute(byte[] masterkey) {
				if (masterkey != null) {
					threemaSafeService.storeMasterKey(masterkey);
					preferenceService.setThreemaSafeServerInfo(mdmConfig.getServerInfo());
					threemaSafeService.setEnabled(true);
					threemaSafeService.uploadNow(true);
				} else {
					Toast.makeText(HomeActivity.this, R.string.safe_error_preparing, Toast.LENGTH_LONG).show();
				}
			}
		}.execute();
	}

	private void showQRPopup() {
		int[] location = getMiniAvatarLocation();

		IdentityPopup identityPopup = new IdentityPopup(this);
		identityPopup.show(this, toolbar, location, () -> {
			// show profile fragment
			bottomNavigationView.post(() -> bottomNavigationView.findViewById(R.id.my_profile).performClick());
		});
	}

	private int[] getMiniAvatarLocation() {
		int[] location = new int[2];
		toolbar.getLocationInWindow(location);

		location[0] += toolbar.getContentInsetLeft() +
			((getResources().getDimensionPixelSize(R.dimen.navigation_icon_padding) +
				getResources().getDimensionPixelSize(R.dimen.navigation_icon_size)) / 2);
		location[1] += toolbar.getHeight() / 2;

		return location;
	}

	private void checkApp() {
		try {
			if (this.currentCheckAppReceiver != null) {
				LocalBroadcastManager.getInstance(this).unregisterReceiver(this.currentCheckAppReceiver);
			}

			if (this.checkLicenseBroadcastReceiver != null) {
				this.unregisterReceiver(this.checkLicenseBroadcastReceiver);
			}
		} catch (IllegalArgumentException r) {
			//not registered... ignore exceptions
		}

		//Register not licensed and update available broadcast
		IntentFilter filter = new IntentFilter();
		filter.addAction(IntentDataUtil.ACTION_LICENSE_NOT_ALLOWED);
		filter.addAction(IntentDataUtil.ACTION_UPDATE_AVAILABLE);
		LocalBroadcastManager.getInstance(this).registerReceiver(currentCheckAppReceiver, filter);

		this.checkLicense();
	}

	private void checkLicense() {
		if (this.isLicenseCheckStarted) {
			return;
		}

		if (serviceManager != null) {
			DeviceService deviceService = serviceManager.getDeviceService();

			if (deviceService != null && deviceService.isOnline()) {
				//start check directly
				CheckLicenseRoutine check;
				try {
					check = new CheckLicenseRoutine(
						this,
						serviceManager.getAPIConnector(),
						serviceManager.getUserService(),
						deviceService,
						serviceManager.getLicenseService(),
						serviceManager.getIdentityStore()
					);
				} catch (FileSystemNotPresentException e) {
					logger.error("Exception", e);
					return;
				}

				new Thread(check).start();
				this.isLicenseCheckStarted = true;

				if (this.checkLicenseBroadcastReceiver != null) {
					try {
						this.unregisterReceiver(this.checkLicenseBroadcastReceiver);
					} catch (IllegalArgumentException e) {
						logger.error("Exception", e);
					}
				}
			} else {
				if (this.checkLicenseBroadcastReceiver == null) {
					this.checkLicenseBroadcastReceiver = new BroadcastReceiver() {
						@Override
						public void onReceive(Context context, Intent intent) {
							logger.debug("receive connectivity change in main activity to check license");
							checkLicense();
						}
					};
					this.registerReceiver(
						this.checkLicenseBroadcastReceiver,
						new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
					);
				}

			}
		}
	}

	@Override
	protected void onDestroy() {
		logger.info("onDestroy");

		ThreemaApplication.activityDestroyed(this);

		try {
			if (this.currentCheckAppReceiver != null) {
				LocalBroadcastManager.getInstance(this).unregisterReceiver(this.currentCheckAppReceiver);
			}

			if (this.checkLicenseBroadcastReceiver != null) {
				this.unregisterReceiver(this.checkLicenseBroadcastReceiver);
			}
		} catch (IllegalArgumentException r) {
			//not registered... ignore exceptions
		}

		// remove listeners to avoid memory leaks
		ListenerManager.messageListeners.remove(this.messageListener);
		ListenerManager.smsVerificationListeners.remove(this.smsVerificationListener);
		ListenerManager.appIconListeners.remove(this.appIconListener);
		ListenerManager.profileListeners.remove(this.profileListener);
		ListenerManager.voipCallListeners.remove(this.voipCallListener);
		if (groupCallManager != null) {
			groupCallManager.removeGeneralGroupCallObserver(groupCallObserver);
		}
		ListenerManager.conversationListeners.remove(this.conversationListener);
		ListenerManager.contactCountListener.remove(this.contactCountListener);

		if (serviceManager != null) {
			serviceManager.getConnection().removeConnectionStateListener(connectionStateListener);
		}

		super.onDestroy();
	}

	private void showErrorTextAndExit(String text) {
		GenericAlertDialog.newInstance(R.string.error, text, R.string.finish, 0)
			.show(getSupportFragmentManager(), DIALOG_TAG_FINISH_UP);
	}

	@SuppressLint("StaticFieldLeak")
	private void runUpdatesAndInitMainActivity(final UpdateSystemService updateSystemService) {
		if (isUpdating) {
			return;
		}

		new AsyncTask<Void, Void, Void>() {
			@Override
			protected void onPreExecute() {
				isUpdating = true;

				GenericProgressDialog.newInstance(R.string.updating_system, R.string.please_wait).show(getSupportFragmentManager(), DIALOG_TAG_UPDATING);
			}

			@Override
			protected Void doInBackground(Void... voids) {
				try {
					updateSystemService.update(new UpdateSystemService.OnSystemUpdateRun() {
						@Override
						public void onStart(final UpdateSystemService.SystemUpdate systemUpdate) {
							logger.info("Running update to " + systemUpdate.getText());
						}

						@Override
						public void onFinished(UpdateSystemService.SystemUpdate systemUpdate, boolean success) {
							if (success) {
								logger.info("System updated to " + systemUpdate.getText());
							} else {
								logger.error("System update to " + systemUpdate.getText() + " failed!");
							}
						}
					});
				} catch (final Exception e) {
					logger.error("Exception", e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void aVoid) {
				DialogUtil.dismissDialog(getSupportFragmentManager(), DIALOG_TAG_UPDATING, true);
				initMainActivity(null);

				isUpdating = false;
			}
		}.execute();
	}

	private void startMainActivity(Bundle savedInstanceState) {
		// at this point the app should be unlocked, licensed and updated
		// therefore Services are now available
		this.serviceManager = ThreemaApplication.getServiceManager();

		if (this.isInitialized) {
			return;
		}

		boolean isAppStart = savedInstanceState == null;

		if (serviceManager != null) {
			this.userService = this.serviceManager.getUserService();
			this.preferenceService = this.serviceManager.getPreferenceService();
			this.notificationService = serviceManager.getNotificationService();
			this.lockAppService = serviceManager.getLockAppService();
			try {
				this.conversationService = serviceManager.getConversationService();
				this.contactService = serviceManager.getContactService();
				this.groupCallManager = serviceManager.getGroupCallManager();
			} catch (Exception e) {
				//
			}

			if (preferenceService == null || notificationService == null || userService == null) {
				finish();
				return;
			}

			// TODO(ANDR-2816): Remove
			preferenceService.removeLastNotificationRationaleShown();

			// reset connectivity status
			preferenceService.setLastOnlineStatus(serviceManager.getDeviceService().isOnline());

			// remove restart notification
			notificationService.cancelRestartNotification();

			ListenerManager.smsVerificationListeners.add(this.smsVerificationListener);
			ListenerManager.messageListeners.add(this.messageListener);
			ListenerManager.appIconListeners.add(this.appIconListener);
			ListenerManager.profileListeners.add(this.profileListener);
			ListenerManager.voipCallListeners.add(this.voipCallListener);
			if (groupCallManager != null) {
				groupCallManager.addGeneralGroupCallObserver(groupCallObserver);
			}
			ListenerManager.conversationListeners.add(this.conversationListener);
			ListenerManager.contactCountListener.add(this.contactCountListener);

			UpdateSystemService updateSystemService = serviceManager.getUpdateSystemService();
			if (updateSystemService.hasUpdates()) {
				this.runUpdatesAndInitMainActivity(updateSystemService);
			} else {
				this.initMainActivity(savedInstanceState);
			}
			if (isAppStart) {
				if (preferenceService.checkForAppUpdate(this)) {
					serviceManager.getTaskManager().schedule(new ApplicationUpdateStepsTask(serviceManager));
				}
			}
		} else {
			RuntimeUtil.runOnUiThread(() -> showErrorTextAndExit(getString(R.string.service_manager_not_available)));
		}
	}

	@UiThread
	private void initMainActivity(@Nullable Bundle savedInstanceState) {
		final boolean isAppStart = savedInstanceState == null;

		// licensing
		checkApp();

		// start wizard if necessary
		if (preferenceService.getWizardRunning() || !userService.hasIdentity()) {
			logger.debug("Missing identity. Wizard running? " + preferenceService.getWizardRunning());

			if (userService.hasIdentity()) {
				startActivity(new Intent(this, WizardBaseActivity.class));
			} else {
				startActivity(new Intent(this, WizardStartActivity.class));
			}
			finish();
			return;
		}

		// set up content
		setContentView(R.layout.activity_home);

		// Wizard complete
		// Write master key now if no passphrase has been set
		if (!ThreemaApplication.getMasterKey().isProtected()) {
			try {
				ThreemaApplication.getMasterKey().setPassphrase(null);
			} catch (Exception e) {
				// better die if something went wrong as the master key may not have been saved
				throw new RuntimeException(e);
			}
		}

		// Set up the action bar.
		initActionBar();

		//init custom icon
		updateAppLogo();

		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setDisplayUseLogoEnabled(false);

		ConfigUtils.setScreenshotsAllowed(this, preferenceService, lockAppService);

		// add connection state listener for displaying colored connection status line above toolbar
		new Thread(() -> {
			if (serviceManager != null) {
				ServerConnection connection = serviceManager.getConnection();
				connection.addConnectionStateListener(connectionStateListener);
				updateConnectionIndicator(connection.getConnectionState());
			}
		}).start();

		// call onPrepareOptionsMenu
		this.invalidateOptionsMenu();

		// Checks on app start
		if (isAppStart) {
			if (BuildFlavor.forceThreemaPush()) {
				preferenceService.setUseThreemaPush(true);
			} else if (!preferenceService.useThreemaPush() && !PushService.servicesInstalled(this)) {
				// If a non-libre build of Threema cannot find push services, fall back to Threema Push
				this.enableThreemaPush();
				if (!ConfigUtils.isAmazonDevice() && !ConfigUtils.isWorkBuild()) {
					RuntimeUtil.runOnUiThread(() -> {
						// Show "push not available" dialog
						int title = R.string.push_not_available_title;
						final String message = getString(R.string.push_not_available_text1) + "\n\n" + getString(R.string.push_not_available_text2, getString(R.string.app_name));
						ShowOnceDialog.newInstance(title, message).show(getSupportFragmentManager(), "nopush");
					});
				}
			}
		}

		this.mainContent = findViewById(R.id.main_content);
		this.toolbarWarningButton = findViewById(R.id.toolbar_warning);
		this.toolbarWarningButton.setOnClickListener(v -> {
			Intent intent = new Intent(HomeActivity.this, ProblemSolverActivity.class);
			problemSolverLauncher.launch(intent);
		});
		this.noticeSMSLayout = findViewById(R.id.notice_sms_layout);
		findViewById(R.id.notice_sms_button_enter_code).setOnClickListener(v -> SMSVerificationDialog.newInstance(userService.getLinkedMobile(true)).show(getSupportFragmentManager(), DIALOG_TAG_VERIFY_CODE));
		findViewById(R.id.notice_sms_button_cancel).setOnClickListener(v -> GenericAlertDialog.newInstance(R.string.verify_title, R.string.really_cancel_verify, R.string.yes, R.string.no)
			.show(getSupportFragmentManager(), DIALOG_TAG_CANCEL_VERIFY));
		this.noticeSMSLayout.setVisibility(
			userService.getMobileLinkingState() == UserService.LinkingState_PENDING ?
				View.VISIBLE : View.GONE);

		initOngoingCallNotice();

		/*
		 * setup fragments
		 */
		String initialFragmentTag = FRAGMENT_TAG_MESSAGES;
		final int initialItemId;
		final Fragment contactsFragment, messagesFragment, profileFragment;

		Intent intent = getIntent();
		if (intent != null && intent.getBooleanExtra(EXTRA_SHOW_CONTACTS, false)) {
			initialFragmentTag = FRAGMENT_TAG_CONTACTS;
			intent.removeExtra(EXTRA_SHOW_CONTACTS);
		}

		if (!isAppStart && savedInstanceState.containsKey(BUNDLE_CURRENT_FRAGMENT_TAG)) {
			// restored session
			initialFragmentTag = savedInstanceState.getString(BUNDLE_CURRENT_FRAGMENT_TAG, initialFragmentTag);

			contactsFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_CONTACTS);
			messagesFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_MESSAGES);
			profileFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG_PROFILE);

			currentFragmentTag = initialFragmentTag;

			switch (initialFragmentTag) {
				case FRAGMENT_TAG_CONTACTS:
					getSupportFragmentManager().beginTransaction().hide(messagesFragment).hide(profileFragment).show(contactsFragment).commit();
					initialItemId = R.id.contacts;
					break;
				case FRAGMENT_TAG_MESSAGES:
					getSupportFragmentManager().beginTransaction().hide(contactsFragment).hide(profileFragment).show(messagesFragment).commit();
					initialItemId = R.id.messages;
					break;
				case FRAGMENT_TAG_PROFILE:
					getSupportFragmentManager().beginTransaction().hide(messagesFragment).hide(contactsFragment).show(profileFragment).commit();
					initialItemId = R.id.my_profile;
					break;
				default:
					initialItemId = R.id.messages;
			}
		} else {
			// new session
			if (conversationService == null || !conversationService.hasConversations()) {
				initialFragmentTag = FRAGMENT_TAG_CONTACTS;
			}

			contactsFragment = new ContactsSectionFragment();
			messagesFragment = new MessageSectionFragment();
			profileFragment = new MyIDFragment();

			FragmentTransaction messagesTransaction = getSupportFragmentManager().beginTransaction().add(R.id.home_container, messagesFragment, FRAGMENT_TAG_MESSAGES);
			FragmentTransaction contactsTransaction = getSupportFragmentManager().beginTransaction().add(R.id.home_container, contactsFragment, FRAGMENT_TAG_CONTACTS);
			FragmentTransaction profileTransaction = getSupportFragmentManager().beginTransaction().add(R.id.home_container, profileFragment, FRAGMENT_TAG_PROFILE);

			currentFragmentTag = initialFragmentTag;

			switch (initialFragmentTag) {
				case FRAGMENT_TAG_CONTACTS:
					initialItemId = R.id.contacts;
					messagesTransaction.hide(messagesFragment);
					messagesTransaction.hide(profileFragment);
					break;
				case FRAGMENT_TAG_MESSAGES:
					initialItemId = R.id.messages;
					messagesTransaction.hide(contactsFragment);
					messagesTransaction.hide(profileFragment);
					break;
				case FRAGMENT_TAG_PROFILE:
					initialItemId = R.id.my_profile;
					messagesTransaction.hide(messagesFragment);
					messagesTransaction.hide(contactsFragment);
					break;
				default:
					// should never happen
					initialItemId = R.id.messages;
			}

			try {
				messagesTransaction.commitAllowingStateLoss();
				contactsTransaction.commitAllowingStateLoss();
				profileTransaction.commitAllowingStateLoss();
			} catch (IllegalStateException e) {
				logger.error("Exception", e);
			}
		}

		this.bottomNavigationView = findViewById(R.id.bottom_navigation);
		this.bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
			showMainContent();

			Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
			if (currentFragment != null) {
				if (item.getItemId() == R.id.contacts) {
					if (!FRAGMENT_TAG_CONTACTS.equals(currentFragmentTag)) {
						getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fast_fade_in, R.anim.fast_fade_out, R.anim.fast_fade_in, R.anim.fast_fade_out).hide(currentFragment).show(contactsFragment).commit();
						currentFragmentTag = FRAGMENT_TAG_CONTACTS;
					}
					return true;
				} else if (item.getItemId() == R.id.messages) {
					if (!FRAGMENT_TAG_MESSAGES.equals(currentFragmentTag)) {
						getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fast_fade_in, R.anim.fast_fade_out, R.anim.fast_fade_in, R.anim.fast_fade_out).hide(currentFragment).show(messagesFragment).commit();
						currentFragmentTag = FRAGMENT_TAG_MESSAGES;
					}
					return true;
				} else if (item.getItemId() == R.id.my_profile) {
					if (!FRAGMENT_TAG_PROFILE.equals(currentFragmentTag)) {
						getSupportFragmentManager().beginTransaction().setCustomAnimations(R.anim.fast_fade_in, R.anim.fast_fade_out, R.anim.fast_fade_in, R.anim.fast_fade_out).hide(currentFragment).show(profileFragment).commit();
						currentFragmentTag = FRAGMENT_TAG_PROFILE;
					}
					return true;
				}
			}
			return false;
		});
		this.bottomNavigationView.post(() -> {
			bottomNavigationView.setSelectedItemId(initialItemId);
		});
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			Drawable background = bottomNavigationView.getBackground();
			if (background instanceof MaterialShapeDrawable) {
				int color = ((MaterialShapeDrawable) background).getResolvedTintColor();
				Window window = getWindow();
				if (window != null) {
					window.setNavigationBarColor(color);
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
						window.setNavigationBarContrastEnforced(false);
					}
				}
			}
		}

		updateBottomNavigation();

		// restore sync adapter account if necessary
		if (preferenceService.isSyncContacts()) {
			if (!userService.checkAccount()) {
				//create account
				userService.getAccount(true);
			}
			userService.enableAccountAutoSync(true);
		}

		isInitialized = true;

		showWhatsNew();

		notificationService.cancelRestoreNotification();

		if (preferenceService.getLastNotificationPermissionRequestTimestamp() == 0) {
			ConfigUtils.requestNotificationPermission(this, notificationPermissionLauncher, preferenceService);
		}
	}

	private void initOngoingCallNotice() {
		this.ongoingCallNotice = findViewById(R.id.ongoing_call_notice);
		updateOngoingCallNotice();
	}

	private void updateOngoingCallNotice() {
		logger.debug("Update ongoing call notice");

		GroupCallController groupCallController = groupCallManager != null
			? groupCallManager.getCurrentGroupCallController()
			: null;

		boolean hasRunningOOCall = VoipCallService.isRunning();
		boolean hasRunningGroupCall = groupCallController != null;

		if (hasRunningOOCall && hasRunningGroupCall) {
			logger.warn("Invalid state: joined 1:1 AND group call, not showing call notice");
			hideOngoingCallNotice();
		} else if (hasRunningOOCall) {
			showOngoingVoipCallNotice();
		} else if (hasRunningGroupCall) {
			showOngoingGroupCallNotice(groupCallController.getDescription());
		} else {
			logger.debug("No ongoing calls, hide notice");
			hideOngoingCallNotice();
		}
	}

	/**
	 * Hides the ongoing call notice not matter what type of called was displayed before
	 */
	private void hideOngoingCallNotice() {
		if (ongoingCallNotice != null) {
			ongoingCallNotice.hide();
		}
	}

	private void hideOngoingVoipCallNotice() {
		if (ongoingCallNotice != null) {
			ongoingCallNotice.hideVoip();
		}
	}

	@AnyThread
	private void showOngoingVoipCallNotice() {
		if (ongoingCallNotice != null) {
			ongoingCallNotice.showVoip();
		}
	}

	@AnyThread
	private void showOngoingGroupCallNotice(@NonNull GroupCallDescription call) {
		if (!ConfigUtils.isGroupCallsEnabled()) {
			return;
		}
		if (ongoingCallNotice != null && groupCallManager != null && groupCallManager.isJoinedCall(call)) {
			ongoingCallNotice.showGroupCall(call, OngoingCallNoticeMode.MODE_GROUP_CALL_JOINED);
		}
	}

	/**
	 * Ensure that Threema Push is enabled in the preferences.
	 */
	private void enableThreemaPush() {
		if (!preferenceService.useThreemaPush()) {
			preferenceService.setUseThreemaPush(true);
			ThreemaPushService.tryStart(logger, ThreemaApplication.getAppContext());
		}
	}

	@SuppressLint("StaticFieldLeak")
	private void updateDrawerImage() {
		if (toolbar != null) {
			new AsyncTask<Void, Void, Drawable>() {
				@Override
				protected Drawable doInBackground(Void... params) {
					Bitmap bitmap = contactService.getAvatar(
						// Create "fake" contact model for own user
						new ContactModel(userService.getIdentity(), userService.getPublicKey()),
						new AvatarOptions.Builder()
							.setReturnPolicy(AvatarOptions.DefaultAvatarPolicy.DEFAULT_FALLBACK)
							.toOptions()
					);
					if (bitmap != null) {
						int size = getResources().getDimensionPixelSize(R.dimen.navigation_icon_size);
						return new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, size, size, true));
					}
					return null;
				}

				@Override
				protected void onPostExecute(Drawable drawable) {
					if (drawable != null) {
						toolbar.setNavigationIcon(drawable);
						toolbar.setNavigationContentDescription(R.string.open_myid_popup);
					}
				}
			}.execute();
		}
	}

	@SuppressLint("StaticFieldLeak")
	private void reallyCancelVerify() {
		AnimationUtil.collapse(noticeSMSLayout, null, true);
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				try {
					userService.unlinkMobileNumber();
				} catch (Exception e) {
					logger.error("Exception", e);
				}
				return null;
			}
		}.execute();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		super.onCreateOptionsMenu(menu);

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_home, menu);

		ConfigUtils.addIconsToOverflowMenu(this, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Intent intent = null;
		final int id = item.getItemId();
		if (id == android.R.id.home) {
			//showQRPopup();
			return true;
		} else if (id == R.id.menu_lock) {
			lockAppService.lock();
			return true;
		} else if (id == R.id.menu_new_group) {
			intent = new Intent(this, GroupAddActivity.class);
		} else if (id == R.id.menu_new_distribution_list) {
			intent = new Intent(this, DistributionListAddActivity.class);
		} else if (id == R.id.group_requests) {
			intent = new Intent(this, OutgoingGroupRequestActivity.class);
		} else if (id == R.id.my_backups) {
			intent = new Intent(this, BackupAdminActivity.class);
		} else if (id == R.id.webclient) {
			intent = new Intent(this, SessionsActivity.class);
		} else if (id == R.id.multi_device) {
			intent = new Intent(this, LinkedDevicesActivity.class);
		} else if (id == R.id.scanner) {
			intent = new Intent(this, BaseQrScannerActivity.class);
		} else if (id == R.id.help) {
			intent = new Intent(this, SupportActivity.class);
		} else if (id == R.id.settings) {
			startActivityForResult(new Intent(this, SettingsActivity.class), ThreemaActivity.ACTIVITY_ID_SETTINGS);
		} else if (id == R.id.directory) {
			intent = new Intent(this, DirectoryActivity.class);
		} else if (id == R.id.threema_channel) {
			confirmThreemaChannel();
		} else if (id == R.id.archived) {
			intent = new Intent(this, ArchiveActivity.class);
		} else if (id == R.id.globalsearch) {
			intent = new Intent(this, GlobalSearchActivity.class);
		} else if (id == R.id.starred_messages) {
			intent = new Intent(this, StarredMessagesActivity.class);
		}

		if (intent != null) {
			startActivity(intent);
		}

		return super.onOptionsItemSelected(item);
	}

	private void initActionBar() {
		toolbar = findViewById(R.id.main_toolbar);
		setSupportActionBar(toolbar);
		actionBar = getSupportActionBar();

		AppCompatImageView toolbarLogoMain = toolbar.findViewById(R.id.toolbar_logo_main);
		ViewGroup.LayoutParams layoutParams = toolbarLogoMain.getLayoutParams();
		layoutParams.height = ConfigUtils.getActionBarSize(this) / 3;
		toolbarLogoMain.setLayoutParams(layoutParams);
		toolbarLogoMain.setImageResource(R.drawable.logo_main);
		toolbarLogoMain.setColorFilter(ConfigUtils.getColorFromAttribute(this, R.attr.colorOnSurface),
			PorterDuff.Mode.SRC_IN);
		toolbarLogoMain.setContentDescription(getString(R.string.logo));
		toolbarLogoMain.setOnClickListener(v -> {
			if (currentFragmentTag != null) {
				Fragment currentFragment = getSupportFragmentManager().findFragmentByTag(currentFragmentTag);
				if (currentFragment != null && currentFragment.isAdded() && !currentFragment.isHidden()) {
					if (currentFragment instanceof ContactsSectionFragment) {
						((ContactsSectionFragment) currentFragment).onLogoClicked();
					} else if (currentFragment instanceof MessageSectionFragment) {
						((MessageSectionFragment) currentFragment).onLogoClicked();
					} else if (currentFragment instanceof MyIDFragment) {
						((MyIDFragment) currentFragment).onLogoClicked();
					}
				}
			}
		});

		updateDrawerImage();
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);

		if (serviceManager != null) {

			MenuItem lockMenuItem = menu.findItem(R.id.menu_lock);
			if (lockMenuItem != null) {
				lockMenuItem.setVisible(
					lockAppService.isLockingEnabled()
				);
			}

			MenuItem privateChatToggleMenuItem = menu.findItem(R.id.menu_toggle_private_chats);
			if (privateChatToggleMenuItem != null) {
				if (preferenceService.isPrivateChatsHidden()) {
					privateChatToggleMenuItem.setIcon(R.drawable.ic_outline_visibility);
					privateChatToggleMenuItem.setTitle(R.string.title_show_private_chats);
				} else {
					privateChatToggleMenuItem.setIcon(R.drawable.ic_outline_visibility_off);
					privateChatToggleMenuItem.setTitle(R.string.title_hide_private_chats);
				}
				ConfigUtils.tintMenuItem(this, privateChatToggleMenuItem, R.attr.colorOnSurface);
			}

			Boolean addDisabled;
			boolean webDisabled = false;

			if (ConfigUtils.isWorkRestricted()) {
				MenuItem backupsMenuItem = menu.findItem(R.id.my_backups);
				if (backupsMenuItem != null) {
					if (AppRestrictionUtil.isBackupsDisabled(this) || (AppRestrictionUtil.isDataBackupsDisabled(this) && ThreemaSafeMDMConfig.getInstance().isBackupDisabled())) {
						backupsMenuItem.setVisible(false);
					}
				}

				addDisabled = AppRestrictionUtil.getBooleanRestriction(getString(R.string.restriction__disable_add_contact));
				webDisabled = AppRestrictionUtil.isWebDisabled(this);
			} else {
				addDisabled = this.contactService != null &&
					this.contactService.getByIdentity(THREEMA_CHANNEL_IDENTITY) != null;
			}

			if (ConfigUtils.isWorkBuild()) {
				MenuItem menuItem = menu.findItem(R.id.directory);
				if (menuItem != null) {
					menuItem.setVisible(ConfigUtils.isWorkDirectoryEnabled());
				}
				menuItem = menu.findItem(R.id.threema_channel);
				if (menuItem != null) {
					menuItem.setVisible(false);
				}
			} else if (addDisabled != null && addDisabled) {
				MenuItem menuItem = menu.findItem(R.id.threema_channel);
				if (menuItem != null) {
					menuItem.setVisible(false);
				}
			}

			if (ConfigUtils.supportsGroupLinks()) {
				MenuItem menuItem = menu.findItem(R.id.scanner);
				if (menuItem != null) {
					menuItem.setVisible(true);
				}

				menuItem = menu.findItem(R.id.group_requests);
				if (menuItem != null) {
					menuItem.setVisible(true);
					menu.setGroupVisible(menuItem.getGroupId(), true);
				}
			}

			MenuItem webclientMenuItem = menu.findItem(R.id.webclient);
			if (webclientMenuItem != null) {
				webclientMenuItem.setVisible(!webDisabled);
			}

			boolean mdMenuItemVisible = serviceManager.getMultiDeviceManager().isMultiDeviceActive()
				|| ConfigUtils.isMultiDeviceEnabled();
			MenuItem mdMenuItem = menu.findItem(R.id.multi_device);
			if (mdMenuItem != null) {
				mdMenuItem.setVisible(mdMenuItemVisible);
			}

			MenuItem starredMessagesItem = menu.findItem(R.id.starred_messages);
			if (starredMessagesItem != null) {
				String starredMessagesString = getString(R.string.starred_messages);
				if (starredMessagesString != null) {
					if (starredMessagesCount > 0) {
						TextAppearanceSpan textAppearanceSpan = new TextAppearanceSpan(getApplicationContext(), R.style.Threema_TextAppearance_StarredMessages_Count);
						String starredMessagesCountString = starredMessagesCount > 99 ? "99+" : Long.toString(starredMessagesCount);
						SpannableString spannableString = new SpannableString(String.format(Locale.US, starredMessagesString + "   %s", starredMessagesCountString));
						spannableString.setSpan(textAppearanceSpan, spannableString.length() - starredMessagesCountString.length(), spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
						starredMessagesItem.setTitle(spannableString);
					} else {
						starredMessagesItem.setTitle(starredMessagesString);
					}
				}
			}
			return true;
		}
		return false;
	}

	private void verifyPhoneCode(final String code) {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				try {
					userService.verifyMobileNumber(code);
				} catch (LinkMobileNoException e) {
					logger.error("Exception", e);
					return getString(R.string.code_invalid);
				} catch (Exception e) {
					logger.error("Exception", e);
					return getString(R.string.verify_failed_summary);
				}
				return null;
			}

			@Override
			protected void onPostExecute(String result) {
				if (result != null) {
					getSupportFragmentManager().beginTransaction().add(SimpleStringAlertDialog.newInstance(R.string.error, result), "ss").commitAllowingStateLoss();
				} else {
					Toast.makeText(HomeActivity.this, getString(R.string.verify_success_text), Toast.LENGTH_LONG).show();
					DialogUtil.dismissDialog(getSupportFragmentManager(), DIALOG_TAG_VERIFY_CODE, true);
				}
			}
		}.execute();
	}

	@Override
	public void onCallRequested(String tag) {
		if (System.currentTimeMillis() < userService.getMobileLinkingTime() + PHONE_REQUEST_DELAY) {
			SimpleStringAlertDialog.newInstance(R.string.verify_phonecall_text, getString(R.string.wait_one_minute)).show(getSupportFragmentManager(), "mi");
		} else {
			GenericAlertDialog.newInstance(R.string.verify_phonecall_text, R.string.prepare_call_message, R.string.ok, R.string.cancel).show(getSupportFragmentManager(), DIALOG_TAG_VERIFY_CODE_CONFIRM);
		}
	}

	private void reallyRequestCall() {
		new AsyncTask<Void, Void, String>() {
			@Override
			protected String doInBackground(Void... params) {
				try {
					userService.makeMobileLinkCall();
				} catch (LinkMobileNoException e) {
					logger.error("Exception", e);
					return e.getMessage();
				} catch (Exception e) {
					logger.error("Exception", e);
					return getString(R.string.verify_failed_summary);
				}
				return null;
			}

			@Override
			protected void onPostExecute(String result) {
				if (!TestUtil.empty(result)) {
					SimpleStringAlertDialog.newInstance(R.string.an_error_occurred, result).show(getSupportFragmentManager(), "le");
				}
			}
		}.execute();
	}

	@Override
	public void onYes(String tag, String code) {
		switch (tag) {
			case DIALOG_TAG_VERIFY_CODE:
				verifyPhoneCode(code);
				break;
			default:
				break;
		}
	}

	@Override
	public void onYes(String tag, Object data) {
		switch (tag) {
			case DIALOG_TAG_VERIFY_CODE_CONFIRM:
				reallyRequestCall();
				break;
			case DIALOG_TAG_CANCEL_VERIFY:
				reallyCancelVerify();
				break;
			case DIALOG_TAG_MASTERKEY_LOCKED:
				startActivityForResult(new Intent(this, UnlockMasterKeyActivity.class), ThreemaActivity.ACTIVITY_ID_UNLOCK_MASTER_KEY);
				break;
			case DIALOG_TAG_SERIAL_LOCKED:
				startActivityForResult(new Intent(this, EnterSerialActivity.class), ThreemaActivity.ACTIVITY_ID_ENTER_SERIAL);
				finish();
				break;
			case DIALOG_TAG_FINISH_UP:
				System.exit(0);
				break;
			case DIALOG_TAG_THREEMA_CHANNEL_VERIFY:
				addThreemaChannel();
				break;
			case DIALOG_TAG_PASSWORD_PRESET_CONFIRM:
				ThreemaSafeService threemaSafeService = getThreemaSafeService();
				if (threemaSafeService != null) {
					reconfigureSafe(threemaSafeService, (ThreemaSafeMDMConfig) data);
					enableSafe(threemaSafeService, (ThreemaSafeMDMConfig) data, null);
				}
				break;
			default:
				break;
		}
	}

	@Override
	public void onNo(String tag) {
	}

	@Override
	public void onNo(String tag, Object data) {
		switch (tag) {
			case DIALOG_TAG_MASTERKEY_LOCKED:
			case DIALOG_TAG_SERIAL_LOCKED:
				finish();
				break;
			case DIALOG_TAG_PASSWORD_PRESET_CONFIRM:
				/* configuration change deferred */
				break;
			default:
				break;
		}
	}

	@Override
	public void onResume() {
		logger.info("onResume");

		if (!isWhatsNewShown) {
			ThreemaApplication.activityResumed(this);
		} else {
			isWhatsNewShown = false;
		}

		MasterKey masterKey = ThreemaApplication.getMasterKey();
		if (masterKey != null && masterKey.isProtected()) {
			if (!PassphraseService.isRunning()) {
				PassphraseService.start(this);
			}
		}

		if (serviceManager != null) {
			new Thread(() -> {
				try {
					FileService fileService = serviceManager.getFileService();
					fileService.cleanTempDirs();
				} catch (FileSystemNotPresentException e) {
					logger.error("Exception", e);
				}
			}).start();

			try {
				new UpdateStarredMessagesTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
			} catch (RejectedExecutionException e) {
				logger.error("Could not execute update starred message task", e);
			}
		}
		super.onResume();

		showMainContent();
		updateWarningButton();
	}

	@Override
	protected void onPause() {
		logger.info("onPause");

		super.onPause();

		ThreemaApplication.activityPaused(this);
	}

	@Override
	public void onUserInteraction() {
		ThreemaApplication.activityUserInteract(this);
		super.onUserInteraction();
	}

	@Override
	protected void onActivityResult(
		int requestCode, int resultCode,
		Intent data) {

		// http://www.androiddesignpatterns.com/2013/08/fragment-transaction-commit-state-loss.html
		super.onActivityResult(requestCode, resultCode, data);

		switch (requestCode) {
			case ThreemaActivity.ACTIVITY_ID_WIZARDFIRST:
				UserService userService = serviceManager.getUserService();
				if (userService != null && userService.hasIdentity()) {
					showMainContent();
					startMainActivity(null);
				} else {
					finish();
				}
				break;

			case ThreemaActivity.ACTIVITY_ID_UNLOCK_MASTER_KEY:
				MasterKey masterKey = ThreemaApplication.getMasterKey();

				if (masterKey != null && masterKey.isLocked()) {
					GenericAlertDialog.newInstance(R.string.master_key_locked,
							R.string.master_key_locked_want_exit,
							R.string.try_again, R.string.cancel)
						.show(getSupportFragmentManager(), DIALOG_TAG_MASTERKEY_LOCKED);
				} else {
					//hide after unlock
					if (IntentDataUtil.hideAfterUnlock(this.getIntent())) {
						this.finish();
						return;
					}
					this.startMainActivity(null);
					showWhatsNew();
				}
				break;
			case ThreemaActivity.ACTIVITY_ID_ENTER_SERIAL:
				if (serviceManager != null) {
					LicenseService licenseService = null;
					try {
						licenseService = serviceManager.getLicenseService();
					} catch (FileSystemNotPresentException e) {
						logger.error("Exception", e);
					}
					if (licenseService != null && !licenseService.isLicensed()) {
						GenericAlertDialog.newInstance(R.string.enter_serial_title,
								R.string.serial_required_want_exit,
								R.string.try_again, R.string.cancel)
							.show(getSupportFragmentManager(), DIALOG_TAG_SERIAL_LOCKED);
					} else {
						this.startMainActivity(null);
					}
				}
				break;
			case ThreemaActivity.ACTIVITY_ID_SETTINGS:
				this.invalidateOptionsMenu();
				break;
			case ThreemaActivity.ACTIVITY_ID_ID_SECTION:
				if (resultCode == ThreemaActivity.RESULT_RESTART) {
					Intent i = getIntent();
					finish();
					startActivity(i);
				}
				break;
			case REQUEST_CODE_WHATSNEW:
				showMainContent();
				break;
			case ThreemaActivity.ACTIVITY_ID_CONTACT_DETAIL:
			case ThreemaActivity.ACTIVITY_ID_GROUP_DETAIL:
			case ThreemaActivity.ACTIVITY_ID_COMPOSE_MESSAGE:
			default:
				break;
		}
	}

	private void updateAppLogo() {
		//update app logo disabled on "not-work" builds
		if(!ConfigUtils.isWorkBuild()) {
			return;
		}
		File customAppIcon = null;
		try {
			customAppIcon = serviceManager.getFileService().getAppLogo(ConfigUtils.getAppThemeSettingFromDayNightMode(ConfigUtils.getCurrentDayNightMode(this)));
		} catch (FileSystemNotPresentException e) {
			logger.error("Exception", e);
		}

		if(customAppIcon != null && customAppIcon.exists() && this.toolbar != null) {
			//set the icon as app icon
			ImageView headerImageView = toolbar.findViewById(R.id.toolbar_logo_main);

			if(headerImageView != null) {
				headerImageView.clearColorFilter();
				headerImageView.setImageBitmap(BitmapUtil.safeGetBitmapFromUri(this, Uri.fromFile(customAppIcon), ConfigUtils.getUsableWidth(getWindowManager()), false, false, false));
			}
		}
	}

	@SuppressLint("StaticFieldLeak")
	public void addThreemaChannel() {
		final MessageService messageService;

		try {
			messageService = serviceManager.getMessageService();
		} catch (Exception e) {
			return;
		}

		new AsyncTask<Void, Void, Exception>() {
			ContactModel newContactModel;

			@Override
			protected void onPreExecute() {
				GenericProgressDialog.newInstance(R.string.threema_channel, R.string.please_wait).show(getSupportFragmentManager(), THREEMA_CHANNEL_IDENTITY);
			}

			@Override
			protected Exception doInBackground(Void... params) {
				try {
					newContactModel = contactService.createContactByIdentity(THREEMA_CHANNEL_IDENTITY, true);
				} catch (Exception e) {
					return e;
				}
				return null;
			}

			@Override
			protected void onPostExecute(Exception exception) {
				DialogUtil.dismissDialog(getSupportFragmentManager(), THREEMA_CHANNEL_IDENTITY, true);

				if (exception == null || exception instanceof EntryAlreadyExistsException) {
					launchThreemaChannelChat();

					if (exception == null) {
						new Thread(() -> {
							try {
								MessageReceiver receiver = contactService.createReceiver(newContactModel);
								if (!getResources().getConfiguration().locale.getLanguage().startsWith("de") && !getResources().getConfiguration().locale.getLanguage().startsWith("gsw")) {
									Thread.sleep(1000);
									messageService.sendText("en", receiver);
									Thread.sleep(500);
								}
								Thread.sleep(1000);
								messageService.sendText(THREEMA_CHANNEL_START_NEWS_COMMAND, receiver);
								Thread.sleep(1500);
								messageService.sendText(ConfigUtils.isWorkBuild() ? THREEMA_CHANNEL_WORK_COMMAND : THREEMA_CHANNEL_START_ANDROID_COMMAND, receiver);
								Thread.sleep(1500);
								messageService.sendText(THREEMA_CHANNEL_INFO_COMMAND, receiver);
							} catch (Exception e) {
								//
							}
						}).start();
					}
				} else {
					Toast.makeText(HomeActivity.this, R.string.internet_connection_required, Toast.LENGTH_LONG).show();
				}
			}
		}.execute();
	}

	private void launchThreemaChannelChat() {
		Intent intent = new Intent(getApplicationContext(), ComposeMessageActivity.class);
		IntentDataUtil.append(THREEMA_CHANNEL_IDENTITY, intent);
		startActivity(intent);
	}

	@AnyThread
	private void updateConnectionIndicator(final ConnectionState connectionState) {
		logger.debug("connectionState = " + connectionState);
		RuntimeUtil.runOnUiThread(() -> {
			connectionIndicator = findViewById(R.id.connection_indicator);
			if (connectionIndicator != null) {
				ConnectionIndicatorUtil.getInstance().updateConnectionIndicator(connectionIndicator, connectionState);
			}
		});
	}

	private void confirmThreemaChannel() {
		if (contactService.getByIdentity(THREEMA_CHANNEL_IDENTITY) == null) {
			GenericAlertDialog.newInstance(R.string.threema_channel, R.string.threema_channel_intro, R.string.ok, R.string.cancel, 0).show(getSupportFragmentManager(), DIALOG_TAG_THREEMA_CHANNEL_VERIFY);
		} else {
			launchThreemaChannelChat();
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		try {
			super.onSaveInstanceState(outState);
		} catch (Exception e) {
			if (logger != null) {
				logger.error("Exception saving state", e);
			}
		}

		if (currentFragmentTag != null) {
			outState.putString(BUNDLE_CURRENT_FRAGMENT_TAG, currentFragmentTag);
		}
	}
}
