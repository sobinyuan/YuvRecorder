package com.sobin.yuvrecorder;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;

/**
 * 系统相关工具
 * 
 */
public class PhoneUtils {

	/**
	 * 获取屏幕分辨率
	 * 
	 * @param mContext
	 * 
	 * @return
	 */
	

	/**
	 * 获取屏幕分辨率
	 * 
	 * @param ctx
	 * @return [0]height [1]width
	 */
	public static int[] getScreenSizeArray(Context ctx) {
		Display display = ((WindowManager) ctx
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		DisplayMetrics dm = new DisplayMetrics();
		display.getMetrics(dm);
		return new int[] { dm.heightPixels, dm.widthPixels };
	}

	// byte[] 转 16进制
	public static String bytesToHexString(byte[] src) {
		StringBuilder stringBuilder = new StringBuilder("");
		if (src == null || src.length <= 0) {
			return null;
		}
		for (int i = 0; i < src.length; i++) {
			int v = src[i] & 0xFF;
			String hv = Integer.toHexString(v);
			if (hv.length() < 2) {
				stringBuilder.append(0);
			}
			stringBuilder.append(hv);
		}
		return stringBuilder.toString();
	}

	////  16进制 转 byte[]
	public static byte[] hexStringToBytes(String hexString) {
		if (hexString == null || hexString.equals("")) {
			return null;
		}
		hexString = hexString.toUpperCase();
		int length = hexString.length() / 2;
		char[] hexChars = hexString.toCharArray();
		byte[] d = new byte[length];
		for (int i = 0; i < length; i++) {
			int pos = i * 2;
			d[i] = (byte) (charToByte(hexChars[pos]) << 4 | charToByte(hexChars[pos + 1]));
		}
		return d;
	}

	/**
	 * Convert char to byte
	 * 
	 * @param c
	 *            char
	 * @return byte
	 */
	private static byte charToByte(char c) {
		return (byte) "0123456789ABCDEF".indexOf(c);
	}
	
	 public static void rotateYUV240SP_Clockwise(byte[] src,byte[] des,int width,int height)  
	 {          
	        int wh = width * height;  
	        //旋转Y   
	        int k = 0;  
	        for(int i=0;i<width;i++) {  
	            for(int j=0;j<height;j++)   
	            {  
	                  des[k] = src[width*(height-j-1) + i];              
	                  k++;  
	            }  
	        }  
	          
	        for(int i=0;i<width;i+=2) {  
	            for(int j=0;j<height/2;j++)   
	            {     
	                  des[k] = src[wh+ width*(height/2-j-1) + i];      
	                  des[k+1]=src[wh + width*(height/2-j-1) + i+1];  
	                  k+=2;  
	            }  
	        }            
	          
	 }
	 public static void rotateYUV240SP_AntiClockwise(byte[] src,byte[] des,int width,int height)  
	 {  	         
	        int wh = width * height;  
	        //旋转Y   
	        int k = 0;  
	        for(int i=0;i<width;i++) {  
	            for(int j=0;j<height;j++)   
	            {  
	                  des[k] = src[width*j + width-i-1];              
	                  k++;  
	            }  
	        }  
	          
	        for(int i=0;i<width;i+=2) {  
	            for(int j=0;j<height/2;j++)   
	            {     
	                  des[k+1] = src[wh+ width*j + width-i-1];      
	                  des[k]=src[wh + width*j + width-(i+1)-1];  
	                  k+=2;  
	            }  
	        } 
	          
	}

	 //it works becuase in YCbCr_420_SP and YCbCr_422_SP, the Y channel is planar and appears first
    public static void rotateYuvData(byte[] rotatedData, byte[] data, int width, int height,int nCase)
    {
    	if( nCase == 0)
    	{
    		rotateYUV240SP_Clockwise(data,rotatedData,width,height);
    	}else
    	{
    		rotateYUV240SP_AntiClockwise(data,rotatedData,width,height);
    	}  	
      
    }

}
