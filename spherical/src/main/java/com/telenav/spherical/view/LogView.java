package com.telenav.spherical.view;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;
import android.widget.TextView;

/**
 * View for log display
 */
public class LogView extends ScrollView {

    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private TextView textView;

    /**
     * Constructor
     * @param context Context
     * @param attrs Argument for resource
     */
    public LogView(Context context, AttributeSet attrs) {
        super(context, attrs);

        setFillViewport(true);
        textView = new TextView(context);
        textView.setBackgroundResource(android.R.color.darker_gray);
        textView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        this.addView(textView);
    }

    /**
     * Log output request method
     * @param newLine Output log
     */
    public void append(CharSequence newLine) {
        textView.append(newLine);
        textView.append(LINE_SEPARATOR);
        fullScroll(FOCUS_DOWN);
    }
}
