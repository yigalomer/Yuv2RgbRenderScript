package com.example.yuv2rgbrenderscript;

import android.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.Menu;
import android.view.MenuItem;

final class OptionsMenu {

	static SharedPreferences sharedPref  ;
	
    public static void createMenu(final Activity activity, Menu menu) {
    	
    	sharedPref = activity.getPreferences(Context.MODE_PRIVATE);
        
  
//        final MenuItem normal = menu.add("Normal View");
//        normal.setIcon(R.drawable.ic_menu_preferences);
//        normal.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
//
//            @Override
//            public boolean onMenuItemClick(MenuItem menuItem) {
//            	
//            	SharedPreferences.Editor editor = sharedPref.edit();
//            	editor.putString("ViewType", "NormalView");
//            	editor.commit();
//                return false;
//            }
//
//        });
//        
        
        final MenuItem blure = menu.add("Blur View");
        blure.setIcon(R.drawable.ic_menu_preferences);
        blure.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
             	SharedPreferences.Editor editor = sharedPref.edit();
            	editor.putString("ViewType", "BlurView");
            	editor.commit();
                return false;
            }

        });
        
        
        
        
        final MenuItem grayScale = menu.add("Gray scale View");
        grayScale.setIcon(R.drawable.ic_menu_preferences);
        grayScale.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {

            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                
               	SharedPreferences.Editor editor = sharedPref.edit();
            	editor.putString("ViewType", "GrayScaleView");
            	editor.commit();
                return false;
            }

        });

    }

}
