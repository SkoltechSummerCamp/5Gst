package ru.scoltech.openran.speedtest.customButtons;

import android.content.Context;
import android.util.AttributeSet;

import ru.scoltech.openran.speedtest.R;

public class ActionButton extends androidx.appcompat.widget.AppCompatButton {
    //current state is at the content description attr

    public ActionButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void setStop() {
        this.setContentDescription("stop");
        this.setBackground(getContext().getDrawable(R.drawable.ic_stop));
    }

    public void setPlay() { // when continue after stopping
        this.setContentDescription("play");
        this.setBackground(getContext().getDrawable(R.drawable.ic_play));
    }

    public void setStart() { // when start from main menu
        this.setContentDescription("start");
        this.setBackground(getContext().getDrawable(R.drawable.ic_play));
    }

    public void setRestart() {
        this.setContentDescription("start");
        this.setBackground(getContext().getDrawable(R.drawable.ic_replay));
    }


}
