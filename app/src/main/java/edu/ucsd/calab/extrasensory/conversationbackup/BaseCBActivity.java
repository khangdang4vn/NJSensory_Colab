package edu.ucsd.calab.extrasensory.conversationbackup;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;

import edu.ucsd.calab.extrasensory.BuildConfig;
import edu.ucsd.calab.extrasensory.R;
import edu.ucsd.calab.extrasensory.sensors.polarandroidblesdk.PolarActivity;

public abstract class BaseCBActivity extends Activity {
    private static final String TAG = BaseCBActivity.class.getSimpleName();

    protected Button exitAppButton;

    protected Helper.MyDialogFrag dfFatal = new Helper.MyDialogFrag();

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getBooleanExtra(Helper.EXIT_ACTION, false)) {
            finish();
            afterFinish();
            return;
        }
        handleIntent(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        dfFatal.tag = "fatal";
        dfFatal.finishId = R.string.prompt_ok;
        dfFatal.msgView = LayoutInflater.from(this).inflate(R.layout.fatal_message, null);
        dfFatal.titleId = R.string.fatal_title;

        try {
            onCreateBaseCallback();

            if (exitAppButton != null) {
                exitAppButton.setOnClickListener(view -> { finish(); afterFinish(); });
            }

            handleIntent(getIntent());
        } catch (Exception exc) {
            handleFatal(exc);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.app_home:
                startActivity(new Intent(BaseCBActivity.this, ConversationBackupActivity.class));
                return true;
          /*  case R.id.app_archives:
                startActivity(new Intent(BaseCBActivity.this, ArchivesActivity.class));
                return true;*/
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    protected void handleIntent(Intent intent) { }
    protected void afterFinish() { }

    protected abstract void onCreateBaseCallback();

    protected void showAboutApp() {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse("market://details?id=" + BuildConfig.APPLICATION_ID));
        startActivity(intent);
    }

    protected void showDialog(Helper.MyDialogFrag d) {
        // Note: isVisible = added, attach to window, and not hidden.
        // Previously, I was getting "Fragment already added" error.
        // error before: java.lang.IllegalStateException: Fragment already added.
        // Now that dialogs are modal, this should not occur.
        // Also, what if added but not visible? Cannot occur based on lifecycle.
        if(!d.isAdded()) d.show(getFragmentManager(), d.tag);
    }

    protected void handleFatal(Exception exc) {
        // show exception in error dialog (which calls finish when done)
        ((TextView)dfFatal.msgView.findViewById(R.id.fatal_message_text)).setText(exc.getMessage());
        showDialog(dfFatal);
    }

}