package br.usp.ime.dspbenchmarking.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class ZipUtil {

	// Compression   
	public static byte[] compress (String str) throws IOException {
		if (str == null || str.length () == 0) {
			return null;
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		GZIPOutputStream gzip = new GZIPOutputStream (out);
		gzip.write(str.getBytes());
		gzip.flush();
		gzip.close ();
		return out.toByteArray();
	}

	// Decompression   
	public static String uncompress (byte[] str) throws IOException {
		if (str == null || str.length == 0) {
			return "";
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream ();
		ByteArrayInputStream in = new ByteArrayInputStream (str);
		GZIPInputStream gunzip = new GZIPInputStream (in);
		byte[] buffer = new byte[256];
		int n;
		while ( (n = gunzip.read(buffer)) >= 0) {
			out.write(buffer, 0, n);
		}
		// ToString () using the platform default encoding, you can also explicitly specify toString ("GBK")   
		return out.toString ();
	}

}
