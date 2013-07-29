package br.usp.ime.dspbenchmarking.utils;


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ZipUtil {


	// Compression   
	public static String compress (String str) throws IOException {
		if (str == null || str.length () == 0) {
			return str;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		GZIPOutputStream gzip = new GZIPOutputStream (out);
		gzip.write(str.getBytes());
		gzip.flush();
		gzip.close ();
		String result = bytesToHex(out.toByteArray());
//		return out.toString ("ISO-8859-1");
		return result;
	}

	// Decompression   
	public static String uncompress (String str) throws IOException {
		if (str == null || str.length () == 0) {
			return str;
		}
		
		byte[] byteArray = hexStringToByteArray(str);
		
//		ByteArrayInputStream in = new ByteArrayInputStream (str.getBytes("ISO-8859-1"));
		ByteArrayInputStream in = new ByteArrayInputStream (byteArray);
		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		GZIPInputStream gunzip = new GZIPInputStream (in);
		byte[] buffer = new byte[256];
		int n;
		while ( (n = gunzip.read(buffer)) >= 0) {
			out.write(buffer, 0, n);
		}
		// ToString () using the platform default encoding, you can also explicitly specify toString ("GBK")   
		return out.toString ();
	}
	
	final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};
	
	public static String bytesToHex(byte[] bytes) {
	    char[] hexChars = new char[bytes.length * 2];
	    int v;
	    for ( int j = 0; j < bytes.length; j++ ) {
	        v = bytes[j] & 0xFF;
	        hexChars[j * 2] = hexArray[v >>> 4];
	        hexChars[j * 2 + 1] = hexArray[v & 0x0F];
	    }
	    return new String(hexChars);
	}
	
	public static byte[] hexStringToByteArray(String s) {
	    int len = s.length();
	    byte[] data = new byte[len / 2];
	    for (int i = 0; i < len; i += 2) {
	        data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
	                             + Character.digit(s.charAt(i+1), 16));
	    }
	    return data;
	}


}
