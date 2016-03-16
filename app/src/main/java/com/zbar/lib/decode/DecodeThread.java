package com.zbar.lib.decode;

import android.os.Handler;
import android.os.Looper;

import com.zbar.lib.CaptureFragment;

import java.util.concurrent.CountDownLatch;

/**
 * 描述: 解码线程
 */
final class DecodeThread extends Thread {

	CaptureFragment fragment;
	private Handler handler;
	private final CountDownLatch handlerInitLatch;

	DecodeThread(CaptureFragment fragment) {
		this.fragment = fragment;
		handlerInitLatch = new CountDownLatch(1);
	}

	Handler getHandler() {
		try {
			handlerInitLatch.await();
		} catch (InterruptedException ie) {
			// continue?
		}
		return handler;
	}

	@Override
	public void run() {
		Looper.prepare();
		handler = new DecodeHandler(fragment);
		handlerInitLatch.countDown();
		Looper.loop();
	}

}
