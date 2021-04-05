/*
 * Copyright (c) 2017. Highlanders LLC - All Rights Reserved
 * Unauthorized copying of this file, via any medium is strictly prohibited.
 * Proprietary and confidential.
 */

package rs.highlande.highlanders_app.utility;


import android.content.Context;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author mbaldrighi on 10/13/2017.
 */
public class FileUtils {

	public static String loadJSONFromAsset(Context context, String fileName) {
		String json;
		try {
			InputStream is = context.getAssets().open(fileName);
			int size = is.available();
			byte[] buffer = new byte[size];
			is.read(buffer);
			is.close();
			json = new String(buffer, "UTF-8");
		} catch (IOException ex) {
			ex.printStackTrace();
			return null;
		}
		return json;
	}

}
