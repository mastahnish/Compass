package myos.jacek.compass;


import android.os.Bundle;


public class PreferenceFragment extends android.preference.PreferenceFragment{



    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.preferences);

    }


}
