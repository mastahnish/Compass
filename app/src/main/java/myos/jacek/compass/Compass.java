package myos.jacek.compass;

import android.app.Application;

/**
 * Created by Jacek on 2016-01-28.
 */
public class Compass extends Application {
    private static Compass ourInstance;

    public boolean NAVIGETE_BASED_ON_USER_INDICATION = false;

    public static Compass getInstance() {
        return ourInstance;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        ourInstance = this;
    }

    public boolean isNAVIGETE_BASED_ON_USER_INDICATION() {
        return NAVIGETE_BASED_ON_USER_INDICATION;
    }

    public void setNAVIGETE_BASED_ON_USER_INDICATION(boolean NAVIGETE_BASED_ON_USER_INDICATION) {
        this.NAVIGETE_BASED_ON_USER_INDICATION = NAVIGETE_BASED_ON_USER_INDICATION;
    }
}
