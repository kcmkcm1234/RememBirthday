package com.kunzisoft.remembirthday.activity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.kunzisoft.remembirthday.R;
import com.kunzisoft.remembirthday.Utility;
import com.kunzisoft.remembirthday.adapter.AutoMessageAdapter;
import com.kunzisoft.remembirthday.adapter.ReminderNotificationsAdapter;
import com.kunzisoft.remembirthday.animation.AnimationCircle;
import com.kunzisoft.remembirthday.database.ContactBuild;
import com.kunzisoft.remembirthday.element.Contact;
import com.kunzisoft.remembirthday.element.DateUnknownYear;
import com.kunzisoft.remembirthday.preference.PreferencesManager;
import com.kunzisoft.remembirthday.task.ActionBirthdayInDatabaseTask;
import com.kunzisoft.remembirthday.task.RemoveBirthdayFromContactTask;

/**
 * Activity who showMessage the details of buddy selected
 */
public class DetailsBuddyFragment extends Fragment {

    private static final String TAG = "DETAILS_BUDDY_FRAGMENT";

    public static final int MODIFY_CONTACT_RESULT_CODE = 1518;

    private Contact contact;

    protected RecyclerView autoMessagesListView;
    protected AutoMessageAdapter autoMessagesAdapter;

    protected RecyclerView remindersListView;
    protected ReminderNotificationsAdapter remindersAdapter;

    private View menuView;
    private AnimationCircle menuAnimationCircle;

    public void setBuddy(Contact currentContact) {
        Bundle args = new Bundle();
        args.putParcelable(BuddyActivity.EXTRA_BUDDY, currentContact);
        setArguments(args);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_details_buddy, container, false);

        TextView dayAndMonthTextView = (TextView) root.findViewById(R.id.fragment_details_buddy_dayAndMonth);
        TextView yearTextView = (TextView) root.findViewById(R.id.fragment_details_buddy_year);
        TextView daysLeftTextView = (TextView) root.findViewById(R.id.fragment_details_buddy_days_left);
        View selectBirthdayButton = root.findViewById(R.id.fragment_details_buddy_container_date);
        menuView = root.findViewById(R.id.fragment_details_buddy_add_menu);

        // Animation init
        menuAnimationCircle = AnimationCircle.build(menuView);

        // List of reminders elements
        remindersListView = (RecyclerView) root.findViewById(R.id.fragment_details_buddy_list_reminders);
        LinearLayoutManager linearLayoutManagerReminder = new LinearLayoutManager(getContext());
        linearLayoutManagerReminder.setOrientation(LinearLayoutManager.VERTICAL);
        remindersListView.setLayoutManager(linearLayoutManagerReminder);

        // List of auto messages elements
        autoMessagesListView = (RecyclerView) root.findViewById(R.id.fragment_details_buddy_list_auto_messages);
        LinearLayoutManager linearLayoutManagerAutoMessage = new LinearLayoutManager(getContext());
        linearLayoutManagerAutoMessage.setOrientation(LinearLayoutManager.VERTICAL);
        autoMessagesListView.setLayoutManager(linearLayoutManagerAutoMessage);

        // Contact attributes
        contact = null;
        if(getArguments()!=null) {
            contact = getArguments().getParcelable(BuddyActivity.EXTRA_BUDDY);
        }
        if(contact != null) {
            // For save memory get RawId only when showMessage details
            setHasOptionsMenu(true);

            ContactBuild.assignRawContactIdToContact(getContext(), contact);

            selectBirthdayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((BirthdayDialogOpen) getActivity()).openDialogSelection(contact.getRawId());
                }
            });

            if(contact.hasBirthday()) {
                // Display date
                DateUnknownYear currentBuddyBirthday = contact.getBirthday();

                // Assign text for day and month
                dayAndMonthTextView.setText(currentBuddyBirthday.toStringMonthAndDay(java.text.DateFormat.FULL));

                // Assign text for year
                if (contact.getBirthday().containsYear()) {
                    yearTextView.setVisibility(View.VISIBLE);
                    yearTextView.setText(currentBuddyBirthday.toStringYear());
                } else {
                    yearTextView.setVisibility(View.GONE);
                    yearTextView.setText("");
                }
                // Number days left before birthday
                Utility.assignDaysRemainingInTextView(daysLeftTextView, contact.getBirthdayDaysRemaining());

                // Animation for menu
                View addButton = root.findViewById(R.id.fragment_details_buddy_add_button);
                addButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menuAnimationCircle
                                .startPoint(menuView.getWidth() - 80, 0)
                                .animate();
                    }
                });

                // Calendar button
                View calendarButton = root.findViewById(R.id.fragment_details_buddy_menu_calendar);
                calendarButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menuAnimationCircle.hide();
                        Utility.openCalendarAt(getActivity(), contact.getNextBirthday());
                    }
                });

                View reminderButton = root.findViewById(R.id.fragment_details_buddy_menu_reminder);
                reminderButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menuAnimationCircle.hide();
                        remindersAdapter.addDefaultItem();
                    }
                });


                View messageButton = root.findViewById(R.id.fragment_details_buddy_menu_message);
                messageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menuAnimationCircle.hide();
                        autoMessagesAdapter.addDefaultItem();
                    }
                });
            } else {
                //TODO Error
            }
        }
        return root;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if(contact != null && contact.hasBirthday()) {
            // Add default reminders and link view to adapter
            remindersAdapter = new ReminderNotificationsAdapter(getContext(), contact.getBirthday());
            int[] defaultDays = PreferencesManager.getDefaultDays(getContext());
            // TODO get items from saved element
            for(int day : defaultDays) {
                remindersAdapter.addDefaultItem(day);
            }
            remindersListView.setAdapter(remindersAdapter);

            // Link auto messages view to adapter
            autoMessagesAdapter = new AutoMessageAdapter(getContext(), contact.getBirthday());
            autoMessagesListView.setAdapter(autoMessagesAdapter);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_details_buddy, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case android.R.id.home:
                getActivity().finish();
                break;
            case R.id.action_modify_contact:
                if(contact != null) {
                    Intent editIntent = new Intent(Intent.ACTION_EDIT);
                    editIntent.setDataAndType(contact.getUri(), ContactsContract.Contacts.CONTENT_ITEM_TYPE);
                    // Response in activity
                    getActivity().startActivityForResult(editIntent, MODIFY_CONTACT_RESULT_CODE);
                }
                break;
            case R.id.action_delete:
                if(contact != null) {
                    AlertDialog.Builder builderDialog = new AlertDialog.Builder(getContext());
                    builderDialog.setTitle(R.string.dialog_select_birthday_title);
                    builderDialog.setMessage(R.string.dialog_delete_birthday_message);
                    builderDialog.setPositiveButton(R.string.button_yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // Delete anniversary in database
                            RemoveBirthdayFromContactTask removeBirthdayFromContactTask =
                                    new RemoveBirthdayFromContactTask(
                                            getActivity(),
                                            contact.getDataAnniversaryId(),
                                            contact.getBirthday());
                            // Response in activity
                            removeBirthdayFromContactTask.setCallbackActionBirthday(
                                    (ActionBirthdayInDatabaseTask.CallbackActionBirthday) getActivity());
                            removeBirthdayFromContactTask.execute();
                        }
                    });
                    builderDialog.setNegativeButton(R.string.button_no, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // User cancelled the dialog
                        }
                    });
                    builderDialog.create().show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}