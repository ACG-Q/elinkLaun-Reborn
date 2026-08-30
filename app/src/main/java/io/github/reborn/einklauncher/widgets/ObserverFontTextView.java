package io.github.reborn.einklauncher.widgets;

import android.content.Context;

import androidx.annotation.Nullable;

import android.util.AttributeSet;
import android.util.TypedValue;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

/**
 * 支持字体大小动态变化的 TextView。
 * 通过 Observer 模式监听字体大小变更并自动更新。
 */

public class ObserverFontTextView extends TextView implements Observer {
  public ObserverFontTextView(Context context) {
    super(context);
  }

  public ObserverFontTextView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  public ObserverFontTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override
  public void update(Observable o, Object arg) {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, (Float) arg);
//    requestLayout();
  }
}
