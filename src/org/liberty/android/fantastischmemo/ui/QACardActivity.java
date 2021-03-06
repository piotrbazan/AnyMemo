/*

Copyright (C) 2012 Haowen Ning

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/
package org.liberty.android.fantastischmemo.ui;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.liberty.android.fantastischmemo.AMActivity;
import org.liberty.android.fantastischmemo.AnyMemoDBOpenHelper;
import org.liberty.android.fantastischmemo.AnyMemoDBOpenHelperManager;
import org.liberty.android.fantastischmemo.R;
import org.liberty.android.fantastischmemo.domain.Card;
import org.liberty.android.fantastischmemo.domain.Option;
import org.liberty.android.fantastischmemo.domain.Setting;
import org.liberty.android.fantastischmemo.service.AnyMemoService;
import org.liberty.android.fantastischmemo.ui.loader.CardTTSUtilLoader;
import org.liberty.android.fantastischmemo.ui.loader.CardTextUtilLoader;
import org.liberty.android.fantastischmemo.ui.loader.MultipleLoaderManager;
import org.liberty.android.fantastischmemo.ui.loader.SettingLoader;
import org.liberty.android.fantastischmemo.utils.CardTTSUtil;
import org.liberty.android.fantastischmemo.utils.CardTextUtil;

import android.content.Intent;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.ClipboardManager;
import android.text.Spannable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

@SuppressWarnings("deprecation")
public abstract class QACardActivity extends AMActivity {
    public static String EXTRA_DBPATH = "dbpath";

    private String dbPath;

    private String dbName;

    private AnyMemoDBOpenHelper dbOpenHelper;

    /* DAOs */
    private Card currentCard;

    private int animationInResId = 0;
    private int animationOutResId = 0;

    private static final int SETTING_LOADER_ID = 0;

    private static final int CARD_TTS_UTIL_LOADER_ID = 1;

    private static final int CARD_TEXT_UTIL_LOADER_ID = 2;

    private Option option;

    private Setting setting;

    private boolean isAnswerShown = true;

    private TextView smallTitleBar;

    private CardTTSUtil cardTTSUtil;

    private CardTextUtil cardTextUtil;

    private GestureLibrary gestureLibrary;

    /**
     * This needs to be defined before onCreate so in onCreate, all loaders will
     * be registered with the right manager.
     */
    private MultipleLoaderManager multipleLoaderManager = new MultipleLoaderManager();

    @Inject
    public void setOption(Option option) {
        this.option = option;
    }

    public CardTTSUtil getCardTTSUtil() {
        return cardTTSUtil;
    }

    /**
     * This is for testing only.
     */
    public void setMultipleLoaderManager(
            MultipleLoaderManager multipleLoaderManager) {
        this.multipleLoaderManager = multipleLoaderManager;
    }

    /**
     * Subclasses should call this method instead of creating
     * a new instance of multipleLoaderManager.
     */
    public MultipleLoaderManager getMultipleLoaderManager() {
        return multipleLoaderManager;
    }


    @Override
    public void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            dbPath = extras.getString(EXTRA_DBPATH);
        }

        dbOpenHelper = AnyMemoDBOpenHelperManager.getHelper(this, dbPath);
        dbName = FilenameUtils.getName(dbPath);

        dbPath = extras.getString(EXTRA_DBPATH);
        setContentView(getContentView());

        // Set teh default animation
        animationInResId = R.anim.slide_left_in;
        animationOutResId = R.anim.slide_left_out;

        // Load gestures
        loadGestures();

        multipleLoaderManager.registerLoaderCallbacks(SETTING_LOADER_ID, new SettingLoaderCallbacks(), false);
        multipleLoaderManager.registerLoaderCallbacks(CARD_TTS_UTIL_LOADER_ID, new CardTTSUtilLoaderCallbacks(), true);
        multipleLoaderManager.registerLoaderCallbacks(CARD_TEXT_UTIL_LOADER_ID, new CardTextUtilLoaderCallbacks(), true);
        multipleLoaderManager.setOnAllLoaderCompletedRunnable(onPostInitRunnable);
        multipleLoaderManager.startLoading(this);
    }
    
    public int getContentView() {
        return R.layout.qa_card_layout;
    }

    protected void setCurrentCard(Card card) {
        currentCard = card;
    }

    protected Card getCurrentCard() {
        return currentCard;
    }

    protected String getDbPath() {
        return dbPath;
    }

    protected String getDbName() {
        return dbName;
    }

    // Important class that display the card using fragment
    // the showAnswer parameter is handled differently on single
    // sided card and double sided card.
    protected void displayCard(boolean showAnswer) {

        // First prepare the text to display

        String questionTypeface = setting.getQuestionFont();
        String answerTypeface = setting.getAnswerFont();

        Setting.Align questionAlign = setting.getQuestionTextAlign();
        Setting.Align answerAlign = setting.getAnswerTextAlign();

        String questionTypefaceValue = null;
        String answerTypefaceValue = null;
        /* Set the typeface of question and answer */
        if (StringUtils.isNotEmpty(questionTypeface)) {
            questionTypefaceValue = questionTypeface;

        }
        if (StringUtils.isNotEmpty(answerTypeface)) {
            answerTypefaceValue = answerTypeface;
        }

        // Handle the QA ratio
        LinearLayout questionLayout = (LinearLayout) findViewById(R.id.question);
        LinearLayout answerLayout = (LinearLayout) findViewById(R.id.answer);
        float qRatio = setting.getQaRatio();
        if (qRatio > 99.0f) {
            answerLayout.setVisibility(View.GONE);
            questionLayout
                    .setLayoutParams(new LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT, 1.0f));
            answerLayout
                    .setLayoutParams(new LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT, 1.0f));
        } else if (qRatio < 1.0f) {
            questionLayout.setVisibility(View.GONE);
            questionLayout
                    .setLayoutParams(new LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT, 1.0f));
            answerLayout
                    .setLayoutParams(new LayoutParams(
                            LayoutParams.MATCH_PARENT,
                            LayoutParams.MATCH_PARENT, 1.0f));
        } else {
            questionLayout.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                    qRatio));
            answerLayout.setLayoutParams(new LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
                    100f - qRatio));
        }

        // Buttons view can be null if it is not decleared in the layout XML
        View buttonsView = findViewById(R.id.buttons_root);

        // Make sure the buttons view are also handling the event for the answer view
        // e. g. clicking on the blank area of the buttons layout to reveal the answer
        // or flip the card.
        if (buttonsView != null) {
            buttonsView.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    onQuestionViewClickListener.onClick(v);
                }
            });
        }
        // Double sided card has no animation and no horizontal line
        if (setting.getCardStyle() == Setting.CardStyle.DOUBLE_SIDED) {
            if (showAnswer) {
                findViewById(R.id.question).setVisibility(View.GONE);
                findViewById(R.id.answer).setVisibility(View.VISIBLE);

                // Also the buttons should match the color.
                // Do not change color if the color is the default color,
                // AnyMemo will use the theme's color instead.
                if (buttonsView != null && !setting.isDefaultColor()) {
                    buttonsView.setBackgroundColor(setting
                            .getAnswerBackgroundColor());
                }
            } else {
                findViewById(R.id.question).setVisibility(View.VISIBLE);
                findViewById(R.id.answer).setVisibility(View.GONE);

                // Also the buttons should match the color.
                if (buttonsView != null && !setting.isDefaultColor()) {
                    buttonsView.setBackgroundColor(setting
                            .getQuestionBackgroundColor());
                }
            }
            findViewById(R.id.horizontal_line).setVisibility(View.GONE);
        }

        // Set the color of the horizontal line
        View horizontalLine = findViewById(R.id.horizontal_line);
        horizontalLine.setBackgroundColor(setting.getSeparatorColor());

        List<Spannable> spannableFields = cardTextUtil
                .getFieldsToDisplay(getCurrentCard());

        // Question spannable
        Spannable sq = spannableFields.get(0);

        // Answer spannable
        Spannable sa = spannableFields.get(1);

        // Finally we generate the fragments
        CardFragment.Builder questionFragmentBuilder = new CardFragment.Builder(sq)
                .setTextAlignment(questionAlign)
                .setTypefaceFromFile(questionTypefaceValue)
                .setTextOnClickListener(onQuestionTextClickListener)
                .setCardOnClickListener(onQuestionViewClickListener)
                .setTextFontSize(setting.getQuestionFontSize())
                .setTypefaceFromFile(setting.getQuestionFont());

        // For default card colors, we will use the theme's color
        // so we do not set the colors here.
        if (!setting.isDefaultColor()) {
            questionFragmentBuilder
                .setTextColor(setting.getQuestionTextColor())
                .setBackgroundColor(setting.getQuestionBackgroundColor());
        }
        CardFragment questionFragment = questionFragmentBuilder.build();

        CardFragment.Builder answerFragmentBuilder = null;

        if (setting.getCardStyle() == Setting.CardStyle.DOUBLE_SIDED
                || showAnswer) {
            answerFragmentBuilder = new CardFragment.Builder(sa)
                    .setTextAlignment(answerAlign)
                    .setTypefaceFromFile(answerTypefaceValue)
                    .setTextOnClickListener(onAnswerTextClickListener)
                    .setCardOnClickListener(onAnswerViewClickListener)
                    .setTextFontSize(setting.getAnswerFontSize())
                    .setTypefaceFromFile(setting.getAnswerFont());
        } else {
            // For "Show answer" text, we do not use the
            // alignment from the settings.
            // It is always center aligned
            answerFragmentBuilder = new CardFragment.Builder(
                    getString(R.string.memo_show_answer))
                    .setTextAlignment(Setting.Align.CENTER)
                    .setTypefaceFromFile(answerTypefaceValue)
                    .setTextOnClickListener(onAnswerTextClickListener)
                    .setCardOnClickListener(onAnswerViewClickListener)
                    .setTextFontSize(setting.getAnswerFontSize())
                    .setTypefaceFromFile(setting.getAnswerFont());
        }

        if (!setting.isDefaultColor()) {
            answerFragmentBuilder
                    .setTextColor(setting.getAnswerTextColor())
                    .setBackgroundColor(setting.getAnswerBackgroundColor());
        }

        CardFragment answerFragment = answerFragmentBuilder.build();

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

        if (setting.getCardStyle() != Setting.CardStyle.DOUBLE_SIDED
                && option.getEnableAnimation()) {
            if (isAnswerShown == false && showAnswer == true) {
                // No animation here.
            } else {
                ft.setCustomAnimations(animationInResId, animationOutResId);
            }
        }
        ft.replace(R.id.question, questionFragment);
        ft.commit();

        ft = getSupportFragmentManager().beginTransaction();

        if (option.getEnableAnimation()) {
            if (setting.getCardStyle() != Setting.CardStyle.DOUBLE_SIDED) {
                if (isAnswerShown == false && showAnswer == true) {
                    ft.setCustomAnimations(0, R.anim.slide_down);
                } else {
                    ft.setCustomAnimations(animationInResId, animationOutResId);
                }
            } else {
                // Animation for double sided cards
                // Current no animation
            }
        }

        ft.replace(R.id.answer, answerFragment);
        ft.commit();

        isAnswerShown = showAnswer;

        // Set up the small title bar
        // It is defualt "GONE" so it won't take any space
        // if there is no text
        smallTitleBar = (TextView) findViewById(R.id.small_title_bar);

        // Only copy to clipboard if answer is show
        // as a feature request:
        // http://code.google.com/p/anymemo/issues/detail?id=239
        if (showAnswer == true) {
            copyToClipboard();
        }

        onPostDisplayCard();
    }

    protected boolean isAnswerShown() {
        return isAnswerShown;
    }

    protected AnyMemoDBOpenHelper getDbOpenHelper() {
        return dbOpenHelper;
    }

    protected Setting getSetting() {
        return setting;
    }

    protected Option getOption() {
        return option;
    }

    // Called when the initalizing finished.
    protected void onPostInit() {
        DialogFragment df = (DialogFragment) getSupportFragmentManager()
            .findFragmentByTag(LoadingProgressFragment.class.toString());
        if (df != null) {
            df.dismiss();
        }
        View buttonsView = findViewById(R.id.buttons_root);
        if (buttonsView != null && !setting.isDefaultColor()) {
            buttonsView.setBackgroundColor(setting
                    .getAnswerBackgroundColor());
        }

    }

    // Called when the initalizing finished.
    protected void onInit() throws Exception {
        // Do nothing

    }

    // Set the card animation, 0 = no animation
    protected void setAnimation(int animationInResId, int animationOutResId) {
        this.animationInResId = animationInResId;
        this.animationOutResId = animationOutResId;
    }

    private class SettingLoaderCallbacks implements
            LoaderManager.LoaderCallbacks<Setting> {

        @Override
        public Loader<Setting> onCreateLoader(int arg0, Bundle arg1) {
            Loader<Setting> loader = new SettingLoader(QACardActivity.this, dbPath);
            loader.forceLoad();
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<Setting> loader , Setting setting) {
            QACardActivity.this.setting = setting;
            multipleLoaderManager.checkAllLoadersCompleted();
        }

        @Override
        public void onLoaderReset(Loader<Setting> arg0) {
            // Do nothing now
        }
    }

    private class CardTTSUtilLoaderCallbacks implements
            LoaderManager.LoaderCallbacks<CardTTSUtil> {
        @Override
        public Loader<CardTTSUtil> onCreateLoader(int arg0, Bundle arg1) {
            Loader<CardTTSUtil> loader = new CardTTSUtilLoader(QACardActivity.this, dbPath);
            loader.forceLoad();
            return loader;
        }

        @Override
        public void onLoadFinished(Loader<CardTTSUtil> loader , CardTTSUtil cardTTSUtil) {
            QACardActivity.this.cardTTSUtil = cardTTSUtil;
            multipleLoaderManager.checkAllLoadersCompleted();
        }

        @Override
        public void onLoaderReset(Loader<CardTTSUtil> arg0) {
            // Do nothing now
        }
    }

    private class CardTextUtilLoaderCallbacks implements
            LoaderManager.LoaderCallbacks<CardTextUtil> {
        @Override
        public Loader<CardTextUtil> onCreateLoader(int arg0, Bundle arg1) {
             Loader<CardTextUtil> loader = new CardTextUtilLoader(QACardActivity.this, dbPath);
             loader.forceLoad();
             return loader;
        }

        @Override
        public void onLoadFinished(Loader<CardTextUtil> loader , CardTextUtil cardTextUtil) {
            QACardActivity.this.cardTextUtil = cardTextUtil;
            multipleLoaderManager.checkAllLoadersCompleted();
        }
        @Override
        public void onLoaderReset(Loader<CardTextUtil> arg0) {
            // Do nothing now
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        AnyMemoDBOpenHelperManager.releaseHelper(dbOpenHelper);

        if (cardTTSUtil != null) {
            cardTTSUtil.release();
        }
        
        multipleLoaderManager.destroy();

        /* Update the widget because StudyActivity can be accessed though widget*/
        Intent myIntent = new Intent(this, AnyMemoService.class);
        myIntent.putExtra("request_code", AnyMemoService.CANCEL_NOTIFICATION
                | AnyMemoService.UPDATE_WIDGET);
        startService(myIntent);
    }

    // Set the small title to display additional informaiton
    public void setSmallTitle(CharSequence text) {
        if (StringUtils.isNotEmpty(text)) {
            smallTitleBar.setText(text);
            smallTitleBar.setVisibility(View.VISIBLE);
        } else {
            smallTitleBar.setVisibility(View.GONE);
        }

    }

    /* Called when the card is displayed. */
    protected void onPostDisplayCard() {
        // Nothing
    }

    protected boolean speakQuestion() {
        cardTTSUtil.speakCardQuestion(getCurrentCard());
        return true;
    }

    protected boolean speakAnswer() {
        cardTTSUtil.speakCardAnswer(getCurrentCard());
        return true;
    }

    private void loadGestures() {
        gestureLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
        if (!gestureLibrary.load()) {
            Log.e(TAG, "Gestures can not be load");
        }

        GestureOverlayView gestureOverlay =  (GestureOverlayView) findViewById(R.id.gesture_overlay);
        gestureOverlay.addOnGesturePerformedListener(onGesturePerformedListener);

        // Set if gestures are enabled if set on preference
        gestureOverlay.setEnabled(option.getGestureEnabled());
    }


    // Default implementation is to handle the double sided card correctly.
    // Return true if the event is handled, else return false
    protected boolean onClickQuestionView() {
        if (setting.getCardStyle() == Setting.CardStyle.DOUBLE_SIDED) {
            displayCard(true);
            return true;
        }
        return false;
    }

    protected boolean onClickAnswerView() {
        if (setting.getCardStyle() == Setting.CardStyle.DOUBLE_SIDED) {
            displayCard(false);
            return true;
        }
        return false;
    }

    protected boolean onClickQuestionText() {
        if (!onClickQuestionView()) {
            speakQuestion();
        }
        return true;
    }

    protected boolean onClickAnswerText() {
        if (!onClickAnswerView()) {
            speakAnswer();
        }
        return true;
    }


    protected void onGestureDetected(GestureName gestureName) {
        // Nothing
    }

    // Return true if handled. Default not handle it.
    // This method will only be called if the volume key shortcut option is enabled.
    protected boolean onVolumeUpKeyPressed() {
        return false;
    }

    // Return true if handled. Default not handle it.
    protected boolean onVolumeDownKeyPressed() {
        return false;
    }

    // Do not handle the key down event. We handle it in onKeyUp
    // This method will only be called if the volume key shortcut option is enabled.
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event){
        if (option.getVolumeKeyShortcut()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return true;
            }

            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                return true;
            }
        }

        return super.onKeyDown(keyCode, event);
    }

    // handle the key event
    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(option.getVolumeKeyShortcut()) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                return onVolumeUpKeyPressed();
            }

            if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                return onVolumeDownKeyPressed();
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    // Copy to clipboard
    protected void copyToClipboard() {
        String copiedText = "";
        switch (option.getCopyClipboard()) {
            case QUESTION:
                copiedText = "" + currentCard.getQuestion();
                break;
            case ANSWER:
                copiedText = "" + currentCard.getAnswer();
                break;
            case BOTH:
                copiedText = "" + currentCard.getQuestion() + " " + currentCard.getAnswer();
                break;
            default:
                copiedText = "";
        }
        if (StringUtils.isNotEmpty(copiedText)) {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            // Some Samsung device doesn't have ClipboardManager. So check
            // the null here to prevent crash.
            if (cm != null) {
                cm.setText(copiedText);
            }
        }
    }

    private Runnable onPostInitRunnable = new Runnable() {
        public void run() {
            onPostInit();
        }
    };

    private CardFragment.OnClickListener onQuestionTextClickListener = new CardFragment.OnClickListener() {

        @Override
        public void onClick(View v) {
            onClickQuestionText();
        }
    };

    private CardFragment.OnClickListener onAnswerTextClickListener = new CardFragment.OnClickListener() {

        @Override
        public void onClick(View v) {
            onClickAnswerText();
        }
    };

    private CardFragment.OnClickListener onQuestionViewClickListener = new CardFragment.OnClickListener() {

        @Override
        public void onClick(View v) {
            onClickQuestionView();
        }
    };
    private CardFragment.OnClickListener onAnswerViewClickListener = new CardFragment.OnClickListener() {

        @Override
        public void onClick(View v) {
            onClickAnswerView();
        }
    };

    private GestureOverlayView.OnGesturePerformedListener onGesturePerformedListener = new GestureOverlayView.OnGesturePerformedListener() {

        @Override
        public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
            List<Prediction> predictions = gestureLibrary.recognize(gesture);
            if (predictions.size() > 0 && predictions.get(0).score > 3.0) {

                GestureName name = GestureName.parse(predictions.get(0).name);
                // Run the callback on the Activity.
                onGestureDetected(name);
            }

        }
    };
}
