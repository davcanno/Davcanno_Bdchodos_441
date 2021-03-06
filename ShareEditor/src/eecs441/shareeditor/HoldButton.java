package eecs441.shareeditor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;

public class HoldButton extends Button {

  private long initialRepeatDelay = 500;
  private long repeatIntervalInMilliseconds = 100;

  private Runnable repeatClick = new Runnable() {
    @Override
    public void run() {

      performClick();
      postDelayed(repeatClick, repeatIntervalInMilliseconds);
    }
  };

  private void commonConstructorCode() {
    this.setOnTouchListener(new OnTouchListener() {
      @Override
      public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction(); 
                if(action == MotionEvent.ACTION_DOWN) 
                {
                  removeCallbacks(repeatClick);
                  performClick();

                  postDelayed(repeatClick, initialRepeatDelay);
                }
                else if(action == MotionEvent.ACTION_UP) {
                  removeCallbacks(repeatClick);
                }
                return true;
      }
    });
  }

  public HoldButton(Context context) {
    super(context);
    commonConstructorCode();
  }
}