package com.cry.downloaddemo;

import android.Manifest;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telecom.Call;
import android.util.Log;
import android.view.View;

import com.tbruyelle.rxpermissions2.RxPermissions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

public class MainActivity extends AppCompatActivity {

    private String url = "http://dldir1.qq.com/weixin/android/weixin6330android920.apk";
    private String TAG = "zzx";
    private int seg_count = 4;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RxPermissions(MainActivity.this)
                        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean aBoolean) throws Exception {
                                if (aBoolean) {

                                    final Request request = new Request.Builder().url(url).build();
                                    OkHttpClient okHttpClient = OkHttpDelegate.getOkHttpClient();
                                    okhttp3.Call call = okHttpClient.newCall(request);
                                    call.enqueue(new Callback() {
                                        @Override
                                        public void onFailure(okhttp3.Call call, IOException e) {
                                            Log.d("ZZX", call.toString());
                                            e.printStackTrace();
                                        }

                                        @Override
                                        public void onResponse(okhttp3.Call call, final Response response) throws IOException {
                                            long total = response.body().contentLength();
                                            File output = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wwx.apk");

                                            Log.e(TAG, "total------>" + total);
                                            long current = 0;
                                            long bufferSize = 1024;
                                            long len = 0;
                                            BufferedSink sink = Okio.buffer(Okio.sink(output));
                                            Buffer buffer = sink.buffer();

                                            BufferedSource source = response.body().source();
                                            while ((len = source.read(buffer, bufferSize)) != -1) {
                                                sink.emit();
                                                current += len;
//                                                Log.i("zzx", "current=" + current);
                                            }

                                            Log.i("zzx", "finish");
                                        }
                                    });
                                }
                            }
                        });
            }
        });

        final DownloadApi main = OkHttpDelegate.main();


        findViewById(R.id.test_header).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Observable.just(url)
                        .subscribeOn(Schedulers.io())
                        .map(new Function<String, Object>() {
                            @Override
                            public Object apply(String s) throws Exception {
                                return main.HEAD_WithIfRange("bytes=0-", null, url).subscribe(new Consumer<retrofit2.Response<Void>>() {
                                    @Override
                                    public void accept(retrofit2.Response<Void> voidResponse) throws Exception {
                                        Log.i("zzx", voidResponse.toString());
                                    }
                                });
                            }
                        }).subscribe();

            }
        });

        //开始离线下载
        findViewById(R.id.btn_c_start).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new RxPermissions(MainActivity.this)
                        .request(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean aBoolean) throws Exception {
                                if (aBoolean) {

                                    final Request request = new Request.Builder().url(url).build();
                                    OkHttpClient okHttpClient = OkHttpDelegate.getOkHttpClient();
                                    okhttp3.Call call = okHttpClient.newCall(request);
                                    call.enqueue(new Callback() {
                                        @Override
                                        public void onFailure(okhttp3.Call call, IOException e) {
                                            Log.d("ZZX", call.toString());
                                            e.printStackTrace();
                                        }

                                        @Override
                                        public void onResponse(okhttp3.Call call, final Response response) throws IOException {
                                            long total = response.body().contentLength();
                                            File output = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wwx.apk");

                                            //得到content_length 开始分片
                                            long l = total / seg_count;
                                            final long[] segCount = new long[seg_count];
                                            for (int i = 0; i < seg_count; i++) {
                                                if (i != seg_count - 1) {
                                                    segCount[i] = l * i;
                                                } else {
                                                    segCount[i] = total;
                                                }
                                            }
                                            long count = 0;
                                            for (int i = 0; i < segCount.length; i++) {
                                                count += segCount[i];
                                            }
                                            Log.i("zzx", Arrays.toString(segCount));
                                            Log.i("zzx ", "count=" + count + ",total=" + count);

                                            //开始创建线程去下载
                                            for (int i = 0; i < seg_count - 1; i++) {
                                                final int index = i;
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        //创建缓存文件
                                                        String range = "bytes=" + segCount[index] + "-" + segCount[index + 1];
                                                        Log.i("zzx", "range=" + range);

                                                        main.download(range, url).subscribeOn(Schedulers.io()).subscribe(new Consumer<retrofit2.Response<ResponseBody>>() {
                                                            @Override
                                                            public void accept(retrofit2.Response<ResponseBody> responseBodyResponse) throws Exception {
                                                                long total = responseBodyResponse.body().contentLength();
                                                                File outputTemp = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "wwx" + index + ".temp");

                                                                Log.e(TAG, "total------>" + total);
                                                                long current = 0;
                                                                long bufferSize = 1024;
                                                                long len = 0;
                                                                BufferedSink sink = Okio.buffer(Okio.sink(outputTemp));
                                                                Buffer buffer = sink.buffer();

                                                                BufferedSource source = responseBodyResponse.body().source();
                                                                while ((len = source.read(buffer, bufferSize)) != -1) {
//                                                                    sink.emit();
                                                                    current += len;
                                                                }
                                                                sink.close();
                                                                source.close();
                                                                Log.i("zzx", "finish");
                                                            }
                                                        });
                                                    }
                                                }).start();
                                            }
                                        }
                                    });
                                }
                            }
                        });

            }
        });

        //暂停任务
        findViewById(R.id.btn_write).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "txt.txt");
                    if (!file.exists()) {
                        boolean createNewFile = file.createNewFile();
                    }
                    BufferedSink buffer = Okio.buffer(Okio.sink(file));
                    buffer.writeUtf8("First write");
                    buffer.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        //暂停任务
        findViewById(R.id.btn_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "txt.txt");
                    File fileCp = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "txt_cp.txt");
                    BufferedSource bufferedSource = Okio.buffer(Okio.source(file));
                    BufferedSink bufferedSink = Okio.buffer(Okio.sink(fileCp));
                    bufferedSource.readAll(bufferedSink);
                    bufferedSink.close();
                    bufferedSource.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });
    }
}
