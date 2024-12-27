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

package ch.threema.app.fragments;

import static android.view.MenuItem.SHOW_AS_ACTION_ALWAYS;
import static android.view.MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW;
import static android.view.MenuItem.SHOW_AS_ACTION_NEVER;
import static ch.threema.app.ThreemaApplication.MAX_PW_LENGTH_BACKUP;
import static ch.threema.app.ThreemaApplication.MIN_PW_LENGTH_BACKUP;
import static ch.threema.app.managers.ListenerManager.conversationListeners;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.SimpleItemAnimator;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.slf4j.Logger;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.threema.app.R;
import ch.threema.app.ThreemaApplication;
import ch.threema.app.activities.ComposeMessageActivity;
import ch.threema.app.activities.ContactDetailActivity;
import ch.threema.app.activities.DistributionListAddActivity;
import ch.threema.app.activities.RecipientListBaseActivity;
import ch.threema.app.activities.ThreemaActivity;
import ch.threema.app.adapters.MessageListAdapter;
import ch.threema.app.adapters.MessageListAdapterItem;
import ch.threema.app.adapters.MessageListViewHolder;
import ch.threema.app.archive.ArchiveActivity;
import ch.threema.app.asynctasks.DeleteDistributionListAsyncTask;
import ch.threema.app.asynctasks.DeleteGroupAsyncTask;
import ch.threema.app.asynctasks.DeleteMyGroupAsyncTask;
import ch.threema.app.asynctasks.EmptyOrDeleteConversationsAsyncTask;
import ch.threema.app.asynctasks.LeaveGroupAsyncTask;
import ch.threema.app.backuprestore.BackupChatService;
import ch.threema.app.collections.Functional;
import ch.threema.app.collections.IPredicateNonNull;
import ch.threema.app.dialogs.CancelableGenericProgressDialog;
import ch.threema.app.dialogs.GenericAlertDialog;
import ch.threema.app.dialogs.PasswordEntryDialog;
import ch.threema.app.dialogs.SelectorDialog;
import ch.threema.app.dialogs.SimpleStringAlertDialog;
import ch.threema.app.listeners.ChatListener;
import ch.threema.app.listeners.ContactListener;
import ch.threema.app.listeners.ContactSettingsListener;
import ch.threema.app.listeners.ConversationListener;
import ch.threema.app.listeners.GroupListener;
import ch.threema.app.listeners.SynchronizeContactsListener;
import ch.threema.app.managers.ListenerManager;
import ch.threema.app.managers.ServiceManager;
import ch.threema.app.messagereceiver.ContactMessageReceiver;
import ch.threema.app.messagereceiver.DistributionListMessageReceiver;
import ch.threema.app.messagereceiver.GroupMessageReceiver;
import ch.threema.app.messagereceiver.MessageReceiver;
import ch.threema.app.preference.SettingsActivity;
import ch.threema.app.routines.SynchronizeContactsRoutine;
import ch.threema.app.services.ContactService;
import ch.threema.app.services.ConversationService;
import ch.threema.app.services.ConversationTagService;
import ch.threema.app.services.ConversationTagServiceImpl;
import ch.threema.app.services.DeadlineListService;
import ch.threema.app.services.DistributionListService;
import ch.threema.app.services.FileService;
import ch.threema.app.services.GroupService;
import ch.threema.app.services.LockAppService;
import ch.threema.app.services.MessageService;
import ch.threema.app.services.PreferenceService;
import ch.threema.app.services.RingtoneService;
import ch.threema.app.services.UserService;
import ch.threema.app.ui.EmptyRecyclerView;
import ch.threema.app.ui.EmptyView;
import ch.threema.app.ui.ResumePauseHandler;
import ch.threema.app.ui.SelectorDialogItem;
import ch.threema.app.utils.AppRestrictionUtil;
import ch.threema.app.utils.ConfigUtils;
import ch.threema.app.utils.DialogUtil;
import ch.threema.app.utils.EditTextUtil;
import ch.threema.app.utils.FileUtil;
import ch.threema.app.utils.HiddenChatUtil;
import ch.threema.app.utils.IntentDataUtil;
import ch.threema.app.utils.LogUtil;
import ch.threema.app.utils.MimeUtil;
import ch.threema.app.utils.RuntimeUtil;
import ch.threema.app.utils.TestUtil;
import ch.threema.app.utils.ViewUtil;
import ch.threema.app.voip.activities.GroupCallActivity;
import ch.threema.app.voip.groupcall.GroupCallManager;
import ch.threema.base.ThreemaException;
import ch.threema.base.utils.LoggingUtil;
import ch.threema.localcrypto.MasterKeyLockedException;
import ch.threema.storage.models.ContactModel;
import ch.threema.storage.models.ConversationModel;
import ch.threema.storage.models.DistributionListModel;
import ch.threema.storage.models.GroupModel;
import ch.threema.storage.models.TagModel;

/**
 * This is one of the tabs in the home screen. It shows the current conversations.
 */
public class MessageSectionFragment extends MainFragment
		implements
			PasswordEntryDialog.PasswordEntryDialogClickListener,
			GenericAlertDialog.DialogClickListener,
			CancelableGenericProgressDialog.ProgressDialogClickListener,
			MessageListAdapter.ItemClickListener,
			SelectorDialog.SelectorDialogClickListener {
	private static final Logger logger = LoggingUtil.getThreemaLogger("MessageSectionFragment");

	private static final int PERMISSION_REQUEST_SHARE_THREAD = 1;
	private static final int ID_RETURN_FROM_SECURITY_SETTINGS = 33211;
	private static final int TEMP_MESSAGES_FILE_DELETE_WAIT_TIME = 2 * 60 * 1000;

	private static final String DIALOG_TAG_PREPARING_MESSAGES = "progressMsgs";
	private static final String DIALOG_TAG_SHARE_CHAT = "shareChat";
	private static final String DIALOG_TAG_REALLY_HIDE_THREAD = "lockC";
	private static final String DIALOG_TAG_HIDE_THREAD_EXPLAIN = "hideEx";
	private static final String DIALOG_TAG_SELECT_DELETE_ACTION = "sel";
	private static final String DIALOG_TAG_REALLY_LEAVE_GROUP = "rlg";
	private static final String DIALOG_TAG_REALLY_DISSOLVE_GROUP = "reallyDissolveGroup";
	private static final String DIALOG_TAG_REALLY_DELETE_MY_GROUP = "rdmg";
	private static final String DIALOG_TAG_REALLY_DELETE_GROUP = "rdgcc";
	private static final String DIALOG_TAG_REALLY_DELETE_DISTRIBUTION_LIST = "rddl";
	private static final String DIALOG_TAG_REALLY_EMPTY_CHAT = "remc";
	private static final String DIALOG_TAG_REALLY_DELETE_CHAT = "rdec";

	private static final int ID_PRIVATE_TO_PUBLIC = 8111;

	private static final int TAG_EMPTY_CHAT = 1;
	private static final int TAG_DELETE_DISTRIBUTION_LIST = 2;
	private static final int TAG_LEAVE_GROUP = 3;
	private static final int TAG_DISSOLVE_GROUP = 4;
	private static final int TAG_DELETE_MY_GROUP = 5;
	private static final int TAG_DELETE_GROUP = 6;
	private static final int TAG_SET_PRIVATE = 7;
	private static final int TAG_UNSET_PRIVATE = 8;
	private static final int TAG_SHARE = 9;
	private static final int TAG_DELETE_LEFT_GROUP = 10;
	private static final int TAG_EDIT_GROUP = 11;
	private static final int TAG_MARK_READ = 12;
	private static final int TAG_MARK_UNREAD = 13;
	private static final int TAG_DELETE_CHAT = 14;

	private static final String BUNDLE_FILTER_QUERY = "filterQuery";
	private static String highlightUid;

	private ServiceManager serviceManager;
	private ConversationService conversationService;
	private ContactService contactService;
	private GroupService groupService;
	private GroupCallManager groupCallManager;
	private MessageService messageService;
	private DistributionListService distributionListService;
	private BackupChatService backupChatService;
	private DeadlineListService mutedChatsListService, mentionOnlyChatsListService, hiddenChatsListService;
	private ConversationTagService conversationTagService;
	private RingtoneService ringtoneService;
	private FileService fileService;
	private PreferenceService preferenceService;
	private LockAppService lockAppService;

	private Activity activity;
	private File tempMessagesFile;
	private MessageListAdapter messageListAdapter;
	private EmptyRecyclerView recyclerView;
	private View loadingView;
	private SearchView searchView;
	private WeakReference<MenuItem> searchMenuItemRef, toggleHiddenMenuItemRef;
	private ResumePauseHandler resumePauseHandler;
	private int currentFullSyncs = 0;
	private String filterQuery;
	private int cornerRadius;
	private TagModel unreadTagModel;
	private final Map<ConversationModel, MessageListAdapterItem> messageListAdapterItemCache = new HashMap<>();

	private @Nullable String myIdentity;

	private ArchiveSnackbar archiveSnackbar;

	private ConversationModel selectedConversation;
	private ExtendedFloatingActionButton floatingButtonView;

	private final Object messageListAdapterLock = new Object();

	private final SynchronizeContactsListener synchronizeContactsListener = new SynchronizeContactsListener() {
		@Override
		public void onStarted(SynchronizeContactsRoutine startedRoutine) {
			if (startedRoutine.fullSync()) {
				currentFullSyncs++;
			}
		}

		@Override
		public void onFinished(SynchronizeContactsRoutine finishedRoutine) {
			if (finishedRoutine.fullSync()) {
				currentFullSyncs--;

				logger.debug("synchronizeContactsListener.onFinished");
				refreshListEvent();
			}
		}

		@Override
		public void onError(SynchronizeContactsRoutine finishedRoutine) {
			if (finishedRoutine.fullSync()) {
				currentFullSyncs--;
				logger.debug("synchronizeContactsListener.onError");
				refreshListEvent();
			}
		}
	};

	private final ConversationListener conversationListener = new ConversationListener() {
		@Override
		public void onNew(final ConversationModel conversationModel) {
			logger.debug("on new conversation");
			if (messageListAdapter != null && recyclerView != null) {
				List<ConversationModel> changedPositions = Collections.singletonList(conversationModel);

				// If the first item of the recycler view is visible, then scroll up
				Integer scrollToPosition = null;
				RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
				if (layoutManager instanceof LinearLayoutManager
					&& ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition() == 0) {
					// By passing a large integer we simulate a "moving up" change that triggers scrolling up
					scrollToPosition = Integer.MAX_VALUE;
				}
				updateList(scrollToPosition, changedPositions, null);
			}
		}

		@Override
		public void onModified(final ConversationModel modifiedConversationModel, final @Nullable Integer oldPosition) {
			logger.debug("on modified conversation. old position = {}", oldPosition);
			if (messageListAdapter == null || recyclerView == null) {
				return;
			}
			synchronized (messageListAdapterItemCache) {
				messageListAdapterItemCache.remove(modifiedConversationModel);
			}

			// Scroll if position changed (to top)
			List<ConversationModel> changedPositions = new ArrayList<>();
			changedPositions.add(modifiedConversationModel);
			updateList(oldPosition, changedPositions, null);
		}

		@Override
		public void onRemoved(final ConversationModel conversationModel) {
			if (messageListAdapter != null) {
				updateList();
			}
		}

		@Override
		public void onModifiedAll() {
			logger.debug("on modified all");
			if (messageListAdapter != null && recyclerView != null) {
				updateList(0, null, new Runnable() {
					@Override
					public void run() {
						RuntimeUtil.runOnUiThread(new Runnable() {
							@Override
							public void run() {
								messageListAdapter.notifyDataSetChanged();
							}
						});
					}
				});
			}
		}
	};

	private final GroupListener groupListener = new GroupListener() {
		@Override
		public void onNewMember(GroupModel group, String newIdentity, int previousMemberCount) {
			// If this user is added to an existing group
			if (groupService != null && myIdentity != null && myIdentity.equals(newIdentity)) {
				fireReceiverUpdate(groupService.createReceiver(group));
			}
		}
	};

	private final ChatListener chatListener = new ChatListener() {
		@Override
		public void onChatOpened(String conversationUid) {
			highlightUid = conversationUid;

			if (isMultiPaneEnabled(activity) && messageListAdapter != null) {
				messageListAdapter.setHighlightItem(conversationUid);
				messageListAdapter.notifyDataSetChanged();
			}
		}
	};

	private final ContactSettingsListener contactSettingsListener = new ContactSettingsListener() {
		@Override
		public void onSortingChanged() {
			//ignore
		}

		@Override
		public void onNameFormatChanged() {
			logger.debug("contactSettingsListener.onNameFormatChanged");
			refreshListEvent();
		}

		@Override
		public void onAvatarSettingChanged() {
			logger.debug("contactSettingsListener.onAvatarSettingChanged");
			refreshListEvent();
		}

		@Override
		public void onInactiveContactsSettingChanged() {

		}

		@Override
		public void onNotificationSettingChanged(String uid) {
			logger.debug("contactSettingsListener.onNotificationSettingChanged");
			refreshListEvent();
		}
	};

	private final ContactListener contactListener = new ContactListener() {
		@Override
		public void onModified(final @NonNull String identity) {
			this.handleChange();
		}

		@Override
		public void onAvatarChanged(ContactModel contactModel) {
			this.handleChange();
		}

		public void handleChange() {
			if (currentFullSyncs <= 0) {
				refreshListEvent();
			}
		}
	};

	final protected boolean requiredInstances() {
		if (!this.checkInstances()) {
			this.instantiate();
		}
		return this.checkInstances();
	}

	protected boolean checkInstances() {
		return TestUtil.required(
				this.serviceManager,
				this.contactListener,
				this.groupService,
				this.groupCallManager,
				this.conversationService,
				this.distributionListService,
				this.fileService,
				this.backupChatService,
				this.mutedChatsListService,
				this.hiddenChatsListService,
				this.ringtoneService,
				this.preferenceService,
				this.lockAppService);
	}

	protected void instantiate() {
		this.serviceManager = ThreemaApplication.getServiceManager();

		if (this.serviceManager != null) {
			try {
				this.contactService = this.serviceManager.getContactService();
				this.groupService = this.serviceManager.getGroupService();
				this.groupCallManager = this.serviceManager.getGroupCallManager();
				this.messageService = this.serviceManager.getMessageService();
				this.conversationService = this.serviceManager.getConversationService();
				this.distributionListService = this.serviceManager.getDistributionListService();
				this.fileService = this.serviceManager.getFileService();
				this.backupChatService = this.serviceManager.getBackupChatService();
				this.mutedChatsListService = this.serviceManager.getMutedChatsListService();
				this.mentionOnlyChatsListService = this.serviceManager.getMentionOnlyChatsListService();
				this.hiddenChatsListService = this.serviceManager.getHiddenChatsListService();
				this.ringtoneService = this.serviceManager.getRingtoneService();
				this.preferenceService = this.serviceManager.getPreferenceService();
				this.conversationTagService = this.serviceManager.getConversationTagService();
				this.lockAppService = this.serviceManager.getLockAppService();
				UserService userService = serviceManager.getUserService();
				if (userService != null) {
					myIdentity = userService.getIdentity();
				}
			} catch (MasterKeyLockedException e) {
				logger.debug("Master Key locked!");
			} catch (ThreemaException e) {
				logger.error("Exception", e);
			}
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		logger.info("onAttach");

		this.activity = activity;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		logger.info("onCreate");

		setRetainInstance(true);
		setHasOptionsMenu(true);

		setupListeners();

		this.resumePauseHandler = ResumePauseHandler.getByActivity(this, this.activity);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		logger.info("onViewCreated");

		try {
			//show loading first
			ViewUtil.show(loadingView, true);

			updateList(null, null, new Runnable() {
				@Override
				public void run() {
					//hide loading
					ViewUtil.show(loadingView, false);
				}
			}, true);
		} catch (Exception e) {
			LogUtil.exception(e, getActivity());
		}

		if (savedInstanceState != null && TestUtil.isEmptyOrNull(filterQuery)) {
			filterQuery = savedInstanceState.getString(BUNDLE_FILTER_QUERY);
		}

		if (messageListAdapter != null) {
			messageListAdapter.setFilterQuery(filterQuery);
		}
	}

	@Override
	public void onDestroyView() {
		logger.info("onDestroyView");

		searchView = null;

		if (searchMenuItemRef != null && searchMenuItemRef.get() != null) {
			searchMenuItemRef.clear();
		}
		messageListAdapter = null;

		super.onDestroyView();
	}

	@Override
	public void onPrepareOptionsMenu(@NonNull Menu menu) {
		super.onPrepareOptionsMenu(menu);

		// move search item to popup if the lock item is visible
		if (this.searchMenuItemRef != null) {
			if (lockAppService != null && lockAppService.isLockingEnabled()) {
				this.searchMenuItemRef.get().setShowAsAction(SHOW_AS_ACTION_NEVER | SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
			} else {
				this.searchMenuItemRef.get().setShowAsAction(SHOW_AS_ACTION_ALWAYS | SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
		logger.debug("onCreateOptionsMenu");

		if (activity != null) {
			if (!isMultiPaneEnabled(activity)) {
				MenuItem searchMenuItem = menu.findItem(R.id.menu_search_messages);

				if (searchMenuItem == null) {
					inflater.inflate(R.menu.fragment_messages, menu);

					if (activity != null && this.isAdded()) {
						searchMenuItem = menu.findItem(R.id.menu_search_messages);
						this.searchView = (SearchView) searchMenuItem.getActionView();

						if (this.searchView != null) {
							if (!TestUtil.isEmptyOrNull(filterQuery)) {
								// restore filter
								MenuItemCompat.expandActionView(searchMenuItem);
								searchView.setQuery(filterQuery, false);
								searchView.clearFocus();
							}
							this.searchView.setQueryHint(getString(R.string.hint_filter_list));
							this.searchView.setOnQueryTextListener(queryTextListener);
						}
					}
				}

				this.searchMenuItemRef = new WeakReference<>(searchMenuItem);

				toggleHiddenMenuItemRef = new WeakReference<>(menu.findItem(R.id.menu_toggle_private_chats));
				if (toggleHiddenMenuItemRef.get() != null) {
					if (isAdded()) {
						toggleHiddenMenuItemRef.get().setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
								@Override
								public boolean onMenuItemClick(MenuItem item) {
									if (preferenceService.isPrivateChatsHidden()) {
										requestUnhideChats();
									} else {
										preferenceService.setPrivateChatsHidden(true);
										updateList(null, null, new Thread(() -> fireSecretReceiverUpdate()));
									}
									return true;
								}
							});
						updateHiddenMenuVisibility();
					}
				}
			}
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	private void requestUnhideChats() {
		HiddenChatUtil.launchLockCheckDialog(this, preferenceService);
	}

	final SearchView.OnQueryTextListener queryTextListener = new SearchView.OnQueryTextListener() {
		@Override
		public boolean onQueryTextChange(String query) {
			filterQuery = query;
			messageListAdapter.setFilterQuery(query);
			updateList(0, null, null);
			return true;
		}

		@Override
		public boolean onQueryTextSubmit(String query) {
			return true;
		}
	};

	private void showConversation(ConversationModel conversationModel, View v) {
		conversationTagService.removeTagAndNotify(conversationModel, unreadTagModel);
		conversationModel.setUnreadCount(0);

		// Close keyboard if search view is expanded
		if (searchView != null && !searchView.isIconified()) {
			EditTextUtil.hideSoftKeyboard(searchView);
		}

		Intent intent = IntentDataUtil.getShowConversationIntent(conversationModel, activity);

		if (intent == null) {
			return;
		}

		if (isMultiPaneEnabled(activity)) {
			if (this.isAdded()) {
				intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivityForResult(intent, ThreemaActivity.ACTIVITY_ID_COMPOSE_MESSAGE);
				activity.overridePendingTransition(0, 0);
			}
		} else {
			activity.startActivityForResult(intent, ThreemaActivity.ACTIVITY_ID_COMPOSE_MESSAGE);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
			case ThreemaActivity.ACTIVITY_ID_SHARE_CHAT:
				if (tempMessagesFile != null) {
				/* We cannot delete the file immediately as some apps (e.g. Dropbox)
				   take some time until they read the file after the intent has been completed.
				   As we can't know for sure when they're done, we simply wait for one minute before
				   we delete the temporary file. */
					new Thread() {
						final String tmpfilePath = tempMessagesFile.getAbsolutePath();

						@Override
						public void run() {
							try {
								Thread.sleep(TEMP_MESSAGES_FILE_DELETE_WAIT_TIME);
							} catch (InterruptedException e) {
								logger.error("Exception", e);
							} finally {
								FileUtil.deleteFileOrWarn(tmpfilePath, "tempMessagesFile", logger);
							}
						}
					}.start();

					tempMessagesFile = null;
				}
				break;
			case ThreemaActivity.ACTIVITY_ID_CHECK_LOCK:
				if (resultCode == Activity.RESULT_OK) {
					serviceManager.getScreenLockService().setAuthenticated(true);
					preferenceService.setPrivateChatsHidden(false);
					updateList(0, null, new Thread(() -> fireSecretReceiverUpdate()));
				}
				break;
			case ID_RETURN_FROM_SECURITY_SETTINGS:
				if (ConfigUtils.hasProtection(preferenceService)) {
					reallyHideChat(selectedConversation);
				}
				break;
			case ID_PRIVATE_TO_PUBLIC:
				if (resultCode == Activity.RESULT_OK) {
					ThreemaApplication.getServiceManager().getScreenLockService().setAuthenticated(true);
					if (selectedConversation != null) {
						doUnhideChat(selectedConversation);
					}
				}
				// fallthrough
			default:
				super.onActivityResult(requestCode, resultCode, data);
		}
	}

	private void doUnhideChat(@NonNull ConversationModel conversationModel) {
		MessageReceiver<?> receiver = conversationModel.getReceiver();
		if (receiver != null && hiddenChatsListService.has(receiver.getUniqueIdString())) {
			hiddenChatsListService.remove(receiver.getUniqueIdString());

			if (getView() != null) {
				Snackbar.make(getView(), R.string.chat_visible, Snackbar.LENGTH_SHORT).show();
			}

			this.fireReceiverUpdate(receiver);
			messageListAdapter.clearSelections();
		}
	}

	private void hideChat(ConversationModel conversationModel) {
		MessageReceiver receiver = conversationModel.getReceiver();

		if (hiddenChatsListService.has(receiver.getUniqueIdString())) {
			if (ConfigUtils.hasProtection(preferenceService)) {
				// persist selection
				selectedConversation = conversationModel;
				HiddenChatUtil.launchLockCheckDialog(null, this, preferenceService, ID_PRIVATE_TO_PUBLIC);
			} else {
				doUnhideChat(conversationModel);
			}
		} else {
			if (ConfigUtils.hasProtection(preferenceService)) {
				GenericAlertDialog dialog = GenericAlertDialog.newInstance(R.string.hide_chat,
						R.string.really_hide_chat_message,
						R.string.ok,
						R.string.cancel);

				dialog.setTargetFragment(this, 0);
				dialog.setData(conversationModel);
				dialog.show(getFragmentManager(), DIALOG_TAG_REALLY_HIDE_THREAD);
			} else {
				GenericAlertDialog dialog = GenericAlertDialog.newInstance(R.string.hide_chat,
						R.string.hide_chat_message_explain,
						R.string.set_lock,
						R.string.cancel);

				dialog.setTargetFragment(this, 0);
				dialog.setData(conversationModel);
				dialog.show(getFragmentManager(), DIALOG_TAG_HIDE_THREAD_EXPLAIN);
			}
		}
	}

	@SuppressLint("StaticFieldLeak")
	private void reallyHideChat(ConversationModel conversationModel) {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected void onPreExecute() {
				if (resumePauseHandler != null) {
					resumePauseHandler.onPause();
				}
			}

			@Override
			protected Boolean doInBackground(Void... params) {
				if (conversationModel != null && conversationModel.getReceiver() != null) {
					hiddenChatsListService.add(conversationModel.getReceiver().getUniqueIdString(), DeadlineListService.DEADLINE_INDEFINITE);
					fireReceiverUpdate(conversationModel.getReceiver());
					return true;
				}
				return false;
			}

			@Override
			protected void onPostExecute(Boolean success) {
				if (success) {
					messageListAdapter.clearSelections();
					if (getView() != null) {
						Snackbar.make(getView(), R.string.chat_hidden, Snackbar.LENGTH_SHORT).show();
					}
					if (resumePauseHandler != null) {
						resumePauseHandler.onResume();
					}
					updateHiddenMenuVisibility();
					if (ConfigUtils.hasProtection(preferenceService) && preferenceService.isPrivateChatsHidden()) {
						updateList(null, null, new Thread(() -> fireSecretReceiverUpdate()));
					}
				} else {
					Toast.makeText(ThreemaApplication.getAppContext(), R.string.an_error_occurred, Toast.LENGTH_SHORT).show();
				}
			}
		}.execute();
	}

	private void shareChat(final ConversationModel conversationModel, final String password, final boolean includeMedia) {
		CancelableGenericProgressDialog progressDialog = CancelableGenericProgressDialog.newInstance(R.string.preparing_messages, 0, R.string.cancel);
		progressDialog.setTargetFragment(this, 0);
		progressDialog.show(getFragmentManager(), DIALOG_TAG_PREPARING_MESSAGES);

		new Thread(new Runnable() {
			@Override
			public void run() {
				tempMessagesFile = FileUtil.getUniqueFile(fileService.getTempPath().getPath(), "threema-chat.zip");
				FileUtil.deleteFileOrWarn(tempMessagesFile, "tempMessagesFile", logger);

				if (backupChatService.backupChatToZip(conversationModel, tempMessagesFile, password, includeMedia)) {

					if (tempMessagesFile != null && tempMessagesFile.exists() && tempMessagesFile.length() > 0) {
						final Intent intent = new Intent(Intent.ACTION_SEND);
						intent.setType(MimeUtil.MIME_TYPE_ZIP);
						intent.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.share_subject, getString(R.string.app_name)));
						intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.chat_history_attached) + "\n\n" + getString(R.string.share_conversation_body));
						intent.putExtra(Intent.EXTRA_STREAM, fileService.getShareFileUri(tempMessagesFile, null));
						intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

						RuntimeUtil.runOnUiThread(() -> {
							DialogUtil.dismissDialog(getFragmentManager(), DIALOG_TAG_PREPARING_MESSAGES, true);
							startActivityForResult(Intent.createChooser(intent, getString(R.string.share_via)), ThreemaActivity.ACTIVITY_ID_SHARE_CHAT);
						});
					}
				} else {
					RuntimeUtil.runOnUiThread(() -> {
						DialogUtil.dismissDialog(getFragmentManager(), DIALOG_TAG_PREPARING_MESSAGES, true);
						SimpleStringAlertDialog.newInstance(R.string.share_via, getString(R.string.an_error_occurred)).
								show(getFragmentManager(), "diskfull");
					});
				}
			}
		}).start();
	}

	private void prepareShareChat(ConversationModel model) {
		PasswordEntryDialog dialogFragment = PasswordEntryDialog.newInstance(
				R.string.share_chat,
				R.string.enter_zip_password_body,
				R.string.password_hint,
				R.string.ok,
				R.string.cancel,
				MIN_PW_LENGTH_BACKUP,
				MAX_PW_LENGTH_BACKUP,
				R.string.backup_password_again_summary,
				0,
				R.string.backup_data_media,
				PasswordEntryDialog.ForgotHintType.NONE);
		dialogFragment.setTargetFragment(this, 0);
		dialogFragment.setData(model);
		dialogFragment.show(getFragmentManager(), DIALOG_TAG_SHARE_CHAT);
	}

	private void refreshListEvent() {
		logger.debug("refreshListEvent reloadData");
		if (this.resumePauseHandler != null) {
			this.resumePauseHandler.runOnActive("refresh_list", new ResumePauseHandler.RunIfActive() {
				@Override
				public void runOnUiThread() {
					if (messageListAdapter == null) {
						return;
					}
					messageListAdapter.notifyDataSetChanged();
				}
			});
		}
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View fragmentView = getView();

		if (fragmentView == null) {
			fragmentView = inflater.inflate(R.layout.fragment_messages, container, false);

			final LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity());

			this.recyclerView = fragmentView.findViewById(R.id.list);
			this.recyclerView.setHasFixedSize(true);
			this.recyclerView.setLayoutManager(linearLayoutManager);
			this.recyclerView.setItemAnimator(new DefaultItemAnimator());

			this.cornerRadius = getResources().getDimensionPixelSize(R.dimen.messagelist_card_corner_radius);

			final ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT|ItemTouchHelper.LEFT) {
				private final VectorDrawableCompat pinIconDrawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_pin, null);
				private final VectorDrawableCompat unpinIconDrawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_pin_outline, null);
				private final VectorDrawableCompat archiveDrawable = VectorDrawableCompat.create(getResources(), R.drawable.ic_archive_outline, null);

				@Override
				public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
					return 0.7f;
				}

				@Override
				public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
					// disable swiping and dragging for footer views
					if (viewHolder.getItemViewType() == MessageListAdapter.TYPE_FOOTER) {
						return makeMovementFlags(0, 0);
					}
					return super.getMovementFlags(recyclerView, viewHolder);
				}

				@Override
				public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
					return false;
				}

				@Override
				public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
					return super.getSwipeDirs(recyclerView, viewHolder);
				}

				@Override
				public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
					// swipe has ended successfully

					// required to clear swipe layout
					messageListAdapter.notifyDataSetChanged();

					final MessageListViewHolder holder = (MessageListViewHolder) viewHolder;
					MessageListAdapterItem messageListAdapterItem = holder.getMessageListAdapterItem();
					ConversationModel conversationModel = messageListAdapterItem != null ? messageListAdapterItem.getConversationModel() : null;
					if (conversationModel == null) {
						logger.error("Conversation model is null");
						return;
					}
					final int oldPosition = conversationModel.getPosition();

					if (direction == ItemTouchHelper.RIGHT) {
						TagModel pinTagModel = conversationTagService.getTagModel(ConversationTagServiceImpl.FIXED_TAG_PIN);

						conversationTagService.toggle(conversationModel, pinTagModel, true);
						conversationModel.setIsPinTagged(!conversationModel.isPinTagged());

						ArrayList<ConversationModel> conversationModels = new ArrayList<>();
						conversationModels.add(conversationModel);

						updateList(
							null,
							conversationModels,
							() -> conversationListeners.handle((ConversationListener listener) -> listener.onModified(conversationModel, oldPosition))
						);
					} else if (direction == ItemTouchHelper.LEFT) {
						conversationService.archive(conversationModel);

						archiveSnackbar = new ArchiveSnackbar(archiveSnackbar, conversationModel);
					}
 				}

				@Override
				public void onChildDraw(@NonNull Canvas canvas, @NonNull RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
					View itemView = viewHolder.itemView;

					if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
						Paint paint = new Paint();

						if (dX > 0) {
							MessageListViewHolder holder = (MessageListViewHolder) viewHolder;
							TagModel pinTagModel = conversationTagService.getTagModel(ConversationTagServiceImpl.FIXED_TAG_PIN);

							MessageListAdapterItem messageListAdapterItem = holder.getMessageListAdapterItem();
							ConversationModel conversationModel = messageListAdapterItem != null ? messageListAdapterItem.getConversationModel() : null;

							VectorDrawableCompat icon = conversationTagService.isTaggedWith(conversationModel, pinTagModel) ? unpinIconDrawable : pinIconDrawable;
							icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());

							String label = conversationTagService.isTaggedWith(conversationModel, pinTagModel) ? getString(R.string.unpin) : getString(R.string.pin);

							paint.setColor(getResources().getColor(R.color.messagelist_pinned_color));
							canvas.drawRect((float) itemView.getLeft(), (float) itemView.getTop(), dX + cornerRadius, (float) itemView.getBottom(), paint);
							canvas.save();
							canvas.translate(
								(float) itemView.getLeft() + getResources().getDimension(R.dimen.swipe_icon_inset),
								(float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - icon.getIntrinsicHeight())/2);
							icon.draw(canvas);
							canvas.restore();

							Paint textPaint = new Paint();
							textPaint.setColor(Color.WHITE);
							textPaint.setTextSize(getResources().getDimension(R.dimen.swipe_text_size));

							Rect rect = new Rect();
							textPaint.getTextBounds(label, 0, label.length(), rect);

							canvas.drawText(label,
								itemView.getLeft() + getResources().getDimension(R.dimen.swipe_text_inset),
								itemView.getTop() + (itemView.getBottom() - itemView.getTop() + rect.height()) / 2,
								textPaint);
						} else if (dX < 0) {
							VectorDrawableCompat icon = archiveDrawable;
							icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
							icon.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN);

							String label = getString(R.string.to_archive);

							paint.setColor(getResources().getColor(R.color.messagelist_archive_color));
							canvas.drawRect(dX, (float) itemView.getTop(), (float) itemView.getRight(), (float) itemView.getBottom(), paint);
							canvas.save();
							canvas.translate(
									(float) itemView.getRight() - getResources().getDimension(R.dimen.swipe_icon_inset) - icon.getIntrinsicWidth(),
									(float) itemView.getTop() + ((float) itemView.getBottom() - (float) itemView.getTop() - icon.getIntrinsicHeight())/2);
							icon.draw(canvas);
							canvas.restore();

							Paint textPaint = new Paint();
							textPaint.setColor(Color.WHITE);
							textPaint.setTextSize(getResources().getDimension(R.dimen.swipe_text_size));

							Rect rect = new Rect();
							textPaint.getTextBounds(label, 0, label.length(), rect);
							float textStartX = itemView.getRight() - getResources().getDimension(R.dimen.swipe_text_inset) - rect.width();
							if (textStartX < 0) {
								textStartX = 0;
							}

							canvas.drawText(label,
									textStartX,
									itemView.getTop() + (itemView.getBottom() - itemView.getTop() + rect.height()) / 2,
									textPaint);
						}
					}
					super.onChildDraw(canvas, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
				}

				@Override
				public float getSwipeEscapeVelocity(float defaultValue) {
					return defaultValue * 20;
				}

				@Override
				public float getSwipeVelocityThreshold(float defaultValue) {
					return defaultValue * 5;
				}
			};
			ItemTouchHelper itemTouchHelper = new ItemTouchHelper(swipeCallback);
			itemTouchHelper.attachToRecyclerView(recyclerView);

			//disable change animation to avoid avatar flicker FX
			((SimpleItemAnimator) this.recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);

			this.loadingView = fragmentView.findViewById(R.id.session_loading);
			ViewUtil.show(this.loadingView, true);

			this.floatingButtonView = fragmentView.findViewById(R.id.floating);
			this.floatingButtonView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					onFABClicked(v);
				}
			});

			// add text view if contact list is empty
			EmptyView emptyView = new EmptyView(activity);
			emptyView.setup(R.string.no_recent_conversations);
			((ViewGroup) recyclerView.getParent()).addView(emptyView);
			recyclerView.setNumHeadersAndFooters(-1);
			recyclerView.setEmptyView(emptyView);
			recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
				@Override
				public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
					super.onScrolled(recyclerView, dx, dy);

					if (linearLayoutManager.findFirstVisibleItemPosition() == 0) {
						floatingButtonView.extend();
					} else {
						floatingButtonView.shrink();
					}
				}
			});
/* TODO(ANDR-2505) this solution does not currently work on Chromebooks.
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && getActivity() != null && getActivity().isInMultiWindowMode()) {
				recyclerView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
					private final int TOUCH_SAFE_AREA_PX = 5;

					// ignore touches at the very left and right edge of the screen to prevent interference with UI gestures
					@Override
					public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
						int width = getResources().getDisplayMetrics().widthPixels;
						int touchX = (int) e.getRawX();

						return touchX < TOUCH_SAFE_AREA_PX || touchX > width - TOUCH_SAFE_AREA_PX;
					}

					@Override
					public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
					}

					@Override
					public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
					}
				});
			}
*/
			//instantiate fragment
			//
			if (!this.requiredInstances()) {
				logger.error("could not instantiate required objects");
			} else {
				this.unreadTagModel = this.conversationTagService.getTagModel(ConversationTagServiceImpl.FIXED_TAG_UNREAD);
			}
		}
		return fragmentView;
	}

	private void onFABClicked(View v) {
		// stop list fling to avoid crashes due to concurrent access to conversation data
		recyclerView.stopScroll();
		Intent intent = new Intent(getContext(), RecipientListBaseActivity.class);
		intent.putExtra(ThreemaApplication.INTENT_DATA_HIDE_RECENTS, true);
		intent.putExtra(RecipientListBaseActivity.INTENT_DATA_MULTISELECT, false);
		intent.putExtra(RecipientListBaseActivity.INTENT_DATA_MULTISELECT_FOR_COMPOSE, true);
		getActivity().startActivityForResult(intent, ThreemaActivity.ACTIVITY_ID_COMPOSE_MESSAGE);
	}

	@Override
	public void onDestroy() {
		this.removeListeners();

		if (this.resumePauseHandler != null) {
			this.resumePauseHandler.onDestroy(this);
		}

		super.onDestroy();
	}

	@Override
	public void onItemClick(View view, int position, ConversationModel model) {
		new Thread(() -> showConversation(model, view)).start();
	}

	@Override
	public void onAvatarClick(View view, int position, ConversationModel model) {
		Intent intent = null;
		if (model.isContactConversation()) {
			intent = new Intent(getActivity(), ContactDetailActivity.class);
			intent.putExtra(ThreemaApplication.INTENT_DATA_CONTACT, model.getContact().getIdentity());
		} else if (model.isGroupConversation()) {
			openGroupDetails(model);
		} else if (model.isDistributionListConversation()) {
			intent = new Intent(getActivity(), DistributionListAddActivity.class);
			intent.putExtra(ThreemaApplication.INTENT_DATA_DISTRIBUTION_LIST, model.getDistributionList().getId());
		}
		if (intent != null) {
			activity.startActivity(intent);
		}
	}

	@Override
	public void onFooterClick(View view) {
		Intent intent = new Intent(getActivity(), ArchiveActivity.class);
		intent.putExtra(ThreemaApplication.INTENT_DATA_ARCHIVE_FILTER, filterQuery);
		getActivity().startActivity(intent);
	}

	@Override
	public void onJoinGroupCallClick(ConversationModel conversationModel) {
		GroupModel group = conversationModel.getGroup();
		if (group != null) {
			startActivity(GroupCallActivity.getJoinCallIntent(requireActivity(), group.getId()));
		}
	}

	private void openGroupDetails(ConversationModel model) {
		GroupModel groupModel = model.getGroup();
		if (groupModel == null) {
			return;
		}
		Intent intent = groupService.getGroupDetailIntent(groupModel, activity);
		activity.startActivity(intent);
	}

	@Override
	public boolean onItemLongClick(View view, int position, ConversationModel conversationModel) {
		if (!isMultiPaneEnabled(activity)) {
			messageListAdapter.toggleItemChecked(conversationModel, position);
			showSelector();
			return true;
		}
		return false;
	}

	@Override
	public void onProgressbarCanceled(String tag) {
		if (this.backupChatService != null) {
			this.backupChatService.cancel();
		}
	}

	@Override
	public void onHiddenChanged(boolean hidden) {
		logger.debug("*** onHiddenChanged: " + hidden);

		if (hidden) {
			if (this.searchView != null && this.searchView.isShown() && this.searchMenuItemRef != null && this.searchMenuItemRef.get() != null) {
				this.searchMenuItemRef.get().collapseActionView();
			}
			if (this.resumePauseHandler != null) {
				this.resumePauseHandler.onPause();
			}
		} else {
			if (this.resumePauseHandler != null) {
				this.resumePauseHandler.onResume();
			}
		}

	}

	@Override
	public void onPause() {
		super.onPause();
		logger.info("*** onPause");

		if (this.resumePauseHandler != null) {
			this.resumePauseHandler.onPause();
		}
	}

	@Override
	public void onResume() {
		logger.info("*** onResume");

		if (this.resumePauseHandler != null) {
			this.resumePauseHandler.onResume();
		}

		if (this.preferenceService != null) {
			if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M &&
					(PreferenceService.LockingMech_SYSTEM.equals(preferenceService.getLockMechanism()))) {
				KeyguardManager keyguardManager = (KeyguardManager) getActivity().getSystemService(Context.KEYGUARD_SERVICE);
				if (!keyguardManager.isDeviceSecure()) {
					Toast.makeText(getActivity(), R.string.no_lockscreen_set, Toast.LENGTH_LONG).show();
					preferenceService.setLockMechanism(PreferenceService.LockingMech_NONE);
					preferenceService.setAppLockEnabled(false);
					preferenceService.setPrivateChatsHidden(false);
					updateList(0, null, null);
				}
			}
		}
		updateHiddenMenuVisibility();

		messageListAdapter.updateDateView();

		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		logger.info("saveInstance");

		if (!TestUtil.isEmptyOrNull(filterQuery)) {
			outState.putString(BUNDLE_FILTER_QUERY, filterQuery);
		}

		super.onSaveInstanceState(outState);
	}

	@Override
	public void onYes(String tag, String text, boolean isChecked, Object data) {
		shareChat((ConversationModel) data, text, isChecked);
	}

	private void showSelector() {
		ArrayList<SelectorDialogItem> labels = new ArrayList<>();
		ArrayList<Integer> tags = new ArrayList<>();

		if (messageListAdapter.getCheckedItemCount() != 1) {
			return;
		}

		ConversationModel conversationModel = messageListAdapter.getCheckedItems().get(0);
		if (conversationModel == null) {
			return;
		}

		MessageReceiver receiver;
		try {
			receiver = conversationModel.getReceiver();
		} catch (Exception e) {
			logger.error("Exception", e);
			return;
		}

		if (receiver == null) {
			return;
		}

		boolean isPrivate = hiddenChatsListService.has(receiver.getUniqueIdString());

		if (conversationModel.hasUnreadMessage() || conversationTagService.isTaggedWith(conversationModel, unreadTagModel)) {
			labels.add(new SelectorDialogItem(getString(R.string.mark_read), R.drawable.ic_outline_visibility));
			tags.add(TAG_MARK_READ);
		} else {
			labels.add(new SelectorDialogItem(getString(R.string.mark_unread), R.drawable.ic_outline_visibility_off));
			tags.add(TAG_MARK_UNREAD);
		}

		if (isPrivate) {
			labels.add(new SelectorDialogItem(getString(R.string.unset_private), R.drawable.ic_outline_shield_24));
			tags.add(TAG_UNSET_PRIVATE);
		} else {
			labels.add(new SelectorDialogItem(getString(R.string.set_private), R.drawable.ic_privacy_outline));
			tags.add(TAG_SET_PRIVATE);
		}

		if (!isPrivate && !AppRestrictionUtil.isExportDisabled(getActivity())) {
			labels.add(new SelectorDialogItem(getString(R.string.share_chat), R.drawable.ic_share_outline));
			tags.add(TAG_SHARE);
		}

		if (conversationModel.getMessageCount() > 0) {
			labels.add(new SelectorDialogItem(getString(R.string.empty_chat_title), R.drawable.ic_outline_delete_sweep));
			tags.add(TAG_EMPTY_CHAT);
		}
		if (conversationModel.isContactConversation()) {
			labels.add(new SelectorDialogItem(getString(R.string.delete_chat_title), R.drawable.ic_delete_outline));
			tags.add(TAG_DELETE_CHAT);
		}

		if (conversationModel.isDistributionListConversation()) {
			// distribution lists
			labels.add(new SelectorDialogItem(getString(R.string.really_delete_distribution_list), R.drawable.ic_delete_outline));
			tags.add(TAG_DELETE_DISTRIBUTION_LIST);
		} else if (conversationModel.isGroupConversation()) {
			// group chats
			GroupModel group = conversationModel.getGroup();
			if (group == null) {
				logger.error("Cannot access the group from the conversation model");
				return;
			}
			boolean isCreator = groupService.isGroupCreator(group);
			boolean isMember = groupService.isGroupMember(group);
			boolean hasOtherMembers = groupService.getOtherMemberCount(group) > 0;
			// Check also if the user is a group member, because orphaned groups should not be
			// editable.
			if (isCreator && isMember) {
				labels.add(new SelectorDialogItem(getString(R.string.group_edit_title), R.drawable.ic_pencil_outline));
				tags.add(TAG_EDIT_GROUP);
			}
			// Members (except the creator) can leave the group
			if (!isCreator && isMember) {
				labels.add(new SelectorDialogItem(getString(R.string.action_leave_group), R.drawable.ic_outline_directions_run));
				tags.add(TAG_LEAVE_GROUP);
			}
			if (isCreator && isMember && hasOtherMembers) {
				labels.add(new SelectorDialogItem(getString(R.string.action_dissolve_group), R.drawable.ic_outline_directions_run));
				tags.add(TAG_DISSOLVE_GROUP);
			}
			labels.add(new SelectorDialogItem(getString(R.string.action_delete_group), R.drawable.ic_delete_outline));
			if (isMember) {
				if (isCreator) {
					tags.add(TAG_DELETE_MY_GROUP);
				} else {
					tags.add(TAG_DELETE_GROUP);
				}
			} else {
				tags.add(TAG_DELETE_LEFT_GROUP);
			}
		}

		SelectorDialog selectorDialog = SelectorDialog.newInstance(receiver.getDisplayName(), labels, tags, getString(R.string.cancel));
		selectorDialog.setData(conversationModel);
		selectorDialog.setTargetFragment(this, 0);
		selectorDialog.show(getFragmentManager(), DIALOG_TAG_SELECT_DELETE_ACTION);
	}

	@SuppressLint("StringFormatInvalid")
	@Override
	public void onClick(String tag, int which, Object data) {
		GenericAlertDialog dialog;

		messageListAdapter.clearSelections();

		final ConversationModel conversationModel = (ConversationModel) data;

		switch (which) {
			case TAG_EMPTY_CHAT:
				dialog = GenericAlertDialog.newInstance(
						R.string.empty_chat_title,
						R.string.empty_chat_confirm,
						R.string.ok,
						R.string.cancel);
				dialog.setData(conversationModel);
				dialog.setTargetFragment(this, 0);
				dialog.show(getFragmentManager(), DIALOG_TAG_REALLY_EMPTY_CHAT);
				break;
			case TAG_DELETE_CHAT:
				dialog = GenericAlertDialog.newInstance(
					R.string.delete_chat_title,
					R.string.delete_chat_confirm,
					R.string.ok,
					R.string.cancel);
				dialog.setData(conversationModel);
				dialog.setTargetFragment(this, 0);
				dialog.show(getFragmentManager(), DIALOG_TAG_REALLY_DELETE_CHAT);
				break;
			case TAG_DELETE_DISTRIBUTION_LIST:
				dialog = GenericAlertDialog.newInstance(
					R.string.really_delete_distribution_list,
					R.string.really_delete_distribution_list_message,
					R.string.ok,
					R.string.cancel);
				dialog.setTargetFragment(this, 0);
				dialog.setData(conversationModel.getDistributionList());
				dialog.show(getFragmentManager(), DIALOG_TAG_REALLY_DELETE_DISTRIBUTION_LIST);
				break;
			case TAG_EDIT_GROUP:
				openGroupDetails(conversationModel);
				break;
			case TAG_LEAVE_GROUP:
				dialog = GenericAlertDialog.newInstance(
					R.string.action_leave_group,
					R.string.really_leave_group_message,
					R.string.ok,
					R.string.cancel);
				dialog.setTargetFragment(this, 0);
				dialog.setData(conversationModel.getGroup());
				dialog.show(getFragmentManager(), DIALOG_TAG_REALLY_LEAVE_GROUP);
				break;
			case TAG_DISSOLVE_GROUP:
				dialog = GenericAlertDialog.newInstance(
					R.string.action_dissolve_group,
					R.string.really_dissolve_group,
					R.string.ok,
					R.string.cancel
				);
				dialog.setTargetFragment(this, 0);
				dialog.setData(conversationModel.getGroup());
				dialog.show(getParentFragmentManager(), DIALOG_TAG_REALLY_DISSOLVE_GROUP);
				break;
			case TAG_DELETE_MY_GROUP:
				dialog = GenericAlertDialog.newInstance(
					R.string.action_dissolve_and_delete_group,
					R.string.delete_my_group_message,
					R.string.ok,
					R.string.cancel
				);
				dialog.setTargetFragment(this, 0);
				dialog.setData(conversationModel.getGroup());
				dialog.show(getFragmentManager(), DIALOG_TAG_REALLY_DELETE_MY_GROUP);
				break;
			case TAG_DELETE_GROUP:
				dialog = GenericAlertDialog.newInstance(
					R.string.action_delete_group,
					R.string.delete_group_message,
					R.string.ok,
					R.string.cancel
				);
				dialog.setTargetFragment(this, 0);
				dialog.setData(conversationModel.getGroup());
				dialog.show(getFragmentManager(), DIALOG_TAG_REALLY_DELETE_GROUP);
				break;
			case TAG_DELETE_LEFT_GROUP:
				dialog = GenericAlertDialog.newInstance(
					R.string.action_delete_group,
					R.string.delete_left_group_message,
					R.string.ok,
					R.string.cancel);
				dialog.setTargetFragment(this, 0);
				dialog.setData(conversationModel.getGroup());
				dialog.show(getFragmentManager(), DIALOG_TAG_REALLY_DELETE_GROUP);
				break;
			case TAG_SET_PRIVATE:
			case TAG_UNSET_PRIVATE:
				hideChat(conversationModel);
				break;
			case TAG_SHARE:
				if (ConfigUtils.requestWriteStoragePermissions(activity, this, PERMISSION_REQUEST_SHARE_THREAD)) {
					prepareShareChat(conversationModel);
				}
				break;
			case TAG_MARK_READ:
				conversationTagService.removeTagAndNotify(conversationModel, unreadTagModel);
				conversationModel.setIsUnreadTagged(false);
				conversationModel.setUnreadCount(0);
				new Thread(() -> messageService.markConversationAsRead(
					conversationModel.getReceiver(),
					serviceManager.getNotificationService())
				).start();
				break;
			case TAG_MARK_UNREAD:
				conversationTagService.addTagAndNotify(conversationModel, unreadTagModel);
				conversationModel.setIsUnreadTagged(true);
				break;
		}
	}

	@Override
	public void onCancel(String tag) {
		messageListAdapter.clearSelections();
	}

	@Override
	public void onNo(String tag) {
		if (DIALOG_TAG_SELECT_DELETE_ACTION.equals(tag)) {
			messageListAdapter.clearSelections();
		}
	}

	@Override
	public void onYes(String tag, Object data) {
		switch (tag) {
			case DIALOG_TAG_REALLY_HIDE_THREAD:
				reallyHideChat((ConversationModel) data);
				break;
			case DIALOG_TAG_HIDE_THREAD_EXPLAIN:
				selectedConversation = (ConversationModel) data;
				Intent intent = new Intent(activity, SettingsActivity.class);
				intent.putExtra(SettingsActivity.EXTRA_SHOW_SECURITY_FRAGMENT, true);
				startActivityForResult(intent, ID_RETURN_FROM_SECURITY_SETTINGS);
				break;
			case DIALOG_TAG_REALLY_LEAVE_GROUP:
				new LeaveGroupAsyncTask((GroupModel) data, groupService, null, this, null).execute();
				break;
			case DIALOG_TAG_REALLY_DISSOLVE_GROUP:
				groupService.dissolveGroupFromLocal((GroupModel) data);
				break;
			case DIALOG_TAG_REALLY_DELETE_MY_GROUP:
				new DeleteMyGroupAsyncTask((GroupModel) data, groupService, null, this, null).execute();
				break;
			case DIALOG_TAG_REALLY_DELETE_GROUP:
				new DeleteGroupAsyncTask((GroupModel) data, groupService, null, this, null).execute();
				break;
			case DIALOG_TAG_REALLY_DELETE_DISTRIBUTION_LIST:
				new DeleteDistributionListAsyncTask((DistributionListModel) data, distributionListService, this, null).execute();
				break;
			case DIALOG_TAG_REALLY_EMPTY_CHAT:
			case DIALOG_TAG_REALLY_DELETE_CHAT:
				final ConversationModel conversationModel = (ConversationModel) data;

				final EmptyOrDeleteConversationsAsyncTask.Mode mode = tag.equals(DIALOG_TAG_REALLY_DELETE_CHAT)
					? EmptyOrDeleteConversationsAsyncTask.Mode.DELETE
					: EmptyOrDeleteConversationsAsyncTask.Mode.EMPTY;
				MessageReceiver<?> receiver = conversationModel.getReceiver();
				if (receiver != null) {
					logger.info("{} chat with receiver {} (type={}).", mode, receiver.getUniqueIdString(), receiver.getType());
				} else {
					logger.warn("Cannot {} chat, receiver is null", mode);
				}
				new EmptyOrDeleteConversationsAsyncTask(
					mode,
					new MessageReceiver[]{ receiver },
					conversationService,
					groupService,
					distributionListService,
					getFragmentManager(),
					null,
					null
				).execute();
				break;
			default:
				break;
		}
	}

	@Override
	public void onNo(String tag, Object data) { }

	@Override
	public boolean onBackPressed() {
		return false;
	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   @NonNull String permissions[], @NonNull int[] grantResults) {
		switch (requestCode) {
			case PERMISSION_REQUEST_SHARE_THREAD:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					prepareShareChat(selectedConversation);
				} else if (!shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
					ConfigUtils.showPermissionRationale(getContext(), getView(), R.string.permission_storage_required);
				}
				break;
		}
	}

	private void setupListeners() {
		logger.debug("*** setup listeners");

		// set listeners
		conversationListeners.add(this.conversationListener);
		ListenerManager.contactListeners.add(this.contactListener);
		ListenerManager.contactSettingsListeners.add(this.contactSettingsListener);
		ListenerManager.synchronizeContactsListeners.add(this.synchronizeContactsListener);
		ListenerManager.chatListener.add(this.chatListener);
		ListenerManager.groupListeners.add(this.groupListener);
	}

	private void removeListeners() {
		logger.debug("*** remove listeners");

		conversationListeners.remove(this.conversationListener);
		ListenerManager.contactListeners.remove(this.contactListener);
		ListenerManager.contactSettingsListeners.remove(this.contactSettingsListener);
		ListenerManager.synchronizeContactsListeners.remove(this.synchronizeContactsListener);
		ListenerManager.chatListener.remove(this.chatListener);
		ListenerManager.groupListeners.remove(this.groupListener);
	}

	private void updateList() {
		this.updateList(null, null, null);
	}

	private void updateList(final Integer scrollToPosition, final List<ConversationModel> changedPositions, final Runnable runAfterSetData) {
		this.updateList(scrollToPosition, changedPositions, runAfterSetData, false);
	}

	@SuppressLint("StaticFieldLeak")
	private void updateList(final Integer scrollToPosition, final List<ConversationModel> changedPositions, final Runnable runAfterSetData, boolean recreate) {
		//require
		if (!this.requiredInstances()) {
			logger.error("could not instantiate required objects");
			return;
		}
		logger.debug("*** update list [" + scrollToPosition + ", " + (changedPositions != null ? changedPositions.size() : "0") + "]");

		Thread updateListThread = new Thread(new Runnable() {
			@Override
			public void run() {
				List<ConversationModel> conversationModels;

				conversationModels = conversationService.getAll(false, new ConversationService.Filter() {
					@Override
					public boolean onlyUnread() {
						return false;
					}

					@Override
					public boolean noDistributionLists() {
						return false;
					}

					@Override
					public boolean noHiddenChats() {
						return preferenceService.isPrivateChatsHidden();
					}

					@Override
					public boolean noInvalid() {
						return false;
					}

					@Override
					public String filterQuery() {
						return filterQuery;
					}
				});

				RuntimeUtil.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						synchronized (messageListAdapterLock) {
							if (messageListAdapter == null || recreate) {
								messageListAdapter = new MessageListAdapter(
									MessageSectionFragment.this.activity,
									contactService,
									groupService,
									distributionListService,
									conversationService,
									mutedChatsListService,
									mentionOnlyChatsListService,
									ringtoneService,
									hiddenChatsListService,
									preferenceService,
									groupCallManager,
									highlightUid,
									MessageSectionFragment.this,
									messageListAdapterItemCache,
									Glide.with(ThreemaApplication.getAppContext())
								);

								recyclerView.setAdapter(messageListAdapter);
							}

							try {
								messageListAdapter.setData(conversationModels, changedPositions);
							} catch (IndexOutOfBoundsException e) {
								logger.debug("Failed to set adapter data", e);
							}
							// make sure footer is refreshed
							messageListAdapter.refreshFooter();

							if (recyclerView != null && scrollToPosition != null) {
								if (changedPositions != null && changedPositions.size() == 1) {
									ConversationModel changedModel = changedPositions.get(0);

									if (changedModel != null && scrollToPosition > changedModel.getPosition() && conversationModels.contains(changedModel)) {
										recyclerView.scrollToPosition(changedModel.getPosition());
									}
								}
							}
						}

						if (runAfterSetData != null) {
							runAfterSetData.run();
						}
					}
				});

				synchronized (messageListAdapterItemCache) {
					for (ConversationModel conversationModel : conversationModels) {
						if (!messageListAdapterItemCache.containsKey(conversationModel)) {
							messageListAdapterItemCache.put(conversationModel, new MessageListAdapterItem(
								conversationModel,
								contactService,
								groupService,
								mutedChatsListService,
								mentionOnlyChatsListService,
								ringtoneService,
								hiddenChatsListService
							));
						}
					}
				}
			}
		});

		if (messageListAdapter == null) {
			// hack: run synchronously when setting up the adapter for the first time to avoid showing an empty list
			updateListThread.run();
		} else {
			updateListThread.start();
		}
	}

	private void updateHiddenMenuVisibility() {
		if (isAdded() && toggleHiddenMenuItemRef != null && toggleHiddenMenuItemRef.get() != null) {
			if (hiddenChatsListService != null) {
				toggleHiddenMenuItemRef.get().setVisible(hiddenChatsListService.getSize() > 0 &&
						ConfigUtils.hasProtection(preferenceService));
				return;
			}
			toggleHiddenMenuItemRef.get().setVisible(false);
		}
	}

	private boolean isMultiPaneEnabled(Activity activity) {
		if (activity != null) {
			return ConfigUtils.isTabletLayout() && activity instanceof ComposeMessageActivity;
		}
		return false;
	}

	private void fireReceiverUpdate(final MessageReceiver receiver) {
		if (receiver instanceof GroupMessageReceiver) {
			ListenerManager.groupListeners.handle(listener ->
				listener.onUpdate(((GroupMessageReceiver) receiver).getGroup())
			);
		} else if (receiver instanceof ContactMessageReceiver) {
			ListenerManager.contactListeners.handle(listener ->
				listener.onModified(((ContactMessageReceiver) receiver).getContact().getIdentity())
			);
		} else if (receiver instanceof DistributionListMessageReceiver) {
			ListenerManager.distributionListListeners.handle(listener ->
				listener.onModify(((DistributionListMessageReceiver) receiver).getDistributionList())
			);
		}
	}

	@WorkerThread
	private void fireSecretReceiverUpdate() {
		//fire a update for every secret receiver (to update webclient data)
		for (ConversationModel c : Functional.filter(this.conversationService.getAll(false, null), new IPredicateNonNull<ConversationModel>() {
			@Override
			public boolean apply(ConversationModel conversationModel) {
				return conversationModel != null && hiddenChatsListService.has(conversationModel.getReceiver().getUniqueIdString());
			}
		})) {
			if (c != null) {
				this.fireReceiverUpdate(c.getReceiver());
			}
		}
	}

	public void onLogoClicked() {
		if (this.recyclerView != null) {
			this.recyclerView.stopScroll();
			this.recyclerView.scrollToPosition(0);
		}
	}

	/**
	 * Keeps track of the last archive chats. This class is used for the undo action.
	 */
	private class ArchiveSnackbar {
		private final Snackbar snackbar;
		private final List<ConversationModel> conversationModels;

		/**
		 * Creates an updated archive snackbar, dismisses the old snackbar (if available), and shows
		 * the updated snackbar.
		 * @param archiveSnackbar the currently shown archive snackbar (if available)
		 * @param archivedConversation the conversation that just has been archived
		 */
		ArchiveSnackbar(@Nullable ArchiveSnackbar archiveSnackbar, ConversationModel archivedConversation) {
			this.conversationModels = new ArrayList<>();
			this.conversationModels.add(archivedConversation);

			if (archiveSnackbar != null) {
				this.conversationModels.addAll(archiveSnackbar.conversationModels);
				archiveSnackbar.dismiss();
			}

			if (getView() != null) {
				int amountArchived = this.conversationModels.size();
				String snackText = ConfigUtils.getSafeQuantityString(getContext(), R.plurals.message_archived, amountArchived, amountArchived, this.conversationModels.size());
				this.snackbar = Snackbar.make(getView(), snackText, 7 * (int) DateUtils.SECOND_IN_MILLIS);
				this.snackbar.setAction(R.string.undo, v -> conversationService.unarchive(conversationModels));
				this.snackbar.addCallback(new Snackbar.Callback() {
					@Override
					public void onDismissed(Snackbar snackbar, int event) {
						super.onDismissed(snackbar, event);
						if (MessageSectionFragment.this.archiveSnackbar == ArchiveSnackbar.this) {
							MessageSectionFragment.this.archiveSnackbar = null;
						}
					}
				});
				this.snackbar.show();
			} else {
				this.snackbar = null;
			}
		}

		void dismiss() {
			if (this.snackbar != null) {
				this.snackbar.dismiss();
			}
		}

	}
}
