package com.example.kaylee.disklrucacheapplication;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.jakewharton.disklrucache.DiskLruCache;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * DiskLruCache磁盘缓存
 */
public class MainActivity extends AppCompatActivity {
    DiskLruCache mDiskLruCache;
    ImageView imgView;
    TextView textView;
    Button btn1,btn2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgView = (ImageView) findViewById(R.id.imageView);
        textView= (TextView) findViewById(R.id.textView);
        btn1= (Button) findViewById(R.id.button);
        btn2= (Button) findViewById(R.id.button2);
        //打开缓存
        openDiskCache();
        //要缓存的图片
        String url = "http://f9.topitme.com/9/37/30/11224703137bb30379o.jpg";
        final String md5key = keyToMd5(url);
        //写缓存
        writeCache();
        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //读缓存
                readImgFromDisk(md5key);
                textView.setText(getCacheSize()+"byte");
            }
        });
        btn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                removeImg(md5key);
                textView.setText(getCacheSize()+"byte");
            }
        });
        btn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCache();
                textView.setText("0byte");

            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mDiskLruCache != null) {
            try {
                //内存中的操作记录同步到日志文件（也就是journal文件）当中
                mDiskLruCache.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (mDiskLruCache != null)
                //将DiskLruCache关闭掉，是和open()方法对应的一个方法。关闭掉了之后就不能再调用DiskLruCache中任何操作缓存数据的方法
                mDiskLruCache.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取缓存到的文件夹
     *
     * @param uniqueName 用于区分不同类型的缓存文件
     * @return
     */
    private File getDiskCacheDir(String uniqueName) {
        String cachePath;
        //如果SD卡存在或者SD卡不可被移除
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable()) {
            //获取到/sdcard/Android/data/<application package>/cache路径
            cachePath = getExternalCacheDir().getPath();
        } else {
            //获取到/data/data/<application package>/cache
            cachePath = getCacheDir().getPath();
        }
        return new File(cachePath + File.separator + uniqueName);
    }

    /**
     * 获取项目的版本号
     *
     * @return
     */
    private int getVersionCode() {

        try {
            PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 1;
    }

    /**
     * 打开磁盘缓存
     *
     * @throws IOException
     */
    private void openDiskCache() {
        //判断该缓存的文件夹存在与否，不存在就创建
        File cacheDir = getDiskCacheDir("bitmap");
        if (!cacheDir.exists()) {
            cacheDir.mkdirs();
        }
        try {
            //每当项目版本号改变，缓存路径下存储的所有数据都会被清除掉，因为DiskLruCache认为当应用程序有版本更新的时候，所有的数据都应该从网上重新获取。
            mDiskLruCache = DiskLruCache.open(cacheDir, getVersionCode(), 1, 10 * 1024 * 1024);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean downloadImgToStream(String imgUrl, OutputStream outputStream) {
        HttpURLConnection connection = null;
        BufferedInputStream in = null;
        BufferedOutputStream out = null;
        try {
            URL url = new URL(imgUrl);
            connection = (HttpURLConnection) url.openConnection();
            in = new BufferedInputStream(connection.getInputStream(), 8 * 1024);
            out = new BufferedOutputStream(outputStream, 8 * 1024);
            int b;
            while ((b = in.read()) != -1) {
                out.write(b);
            }
            return true;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 将字符串转成MD5格式
     *
     * @param key
     * @return
     */
    private String keyToMd5(String key) {
        String md5Ksey = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("MD5");
            messageDigest.update(key.getBytes());
            md5Ksey = bytesToHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            md5Ksey = String.valueOf(key.hashCode());
        }
        return md5Ksey;

    }

    /**
     * 将byte数组转成十六位字符串
     *
     * @param bytes
     * @return
     */
    private String bytesToHexString(byte[] bytes) {
        StringBuffer stringBuffer = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xff);
            if (hex.length() == 1) {
                stringBuffer.append("0");
            }
            stringBuffer.append(hex);
        }
        return stringBuffer.toString();
    }

    /**
     * 将图片写入磁盘中
     */
    private void writeCache() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String url = "http://f9.topitme.com/9/37/30/11224703137bb30379o.jpg";
                final String md5key = keyToMd5(url);
                try {
                    DiskLruCache.Editor edit = mDiskLruCache.edit(md5key);
                    if (edit != null) {
                        if (downloadImgToStream(url, edit.newOutputStream(0))) {
                            edit.commit();
                        } else {
                            edit.abort();
                        }
                    }
                    mDiskLruCache.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 从磁盘中读取文件()
     */
    private void readImgFromDisk(String md5Key) {
        try {
            DiskLruCache.Snapshot snapshot = mDiskLruCache.get(md5Key);
            if (snapshot != null) {
                InputStream in = snapshot.getInputStream(0);
                Bitmap bitmap = BitmapFactory.decodeStream(in);
                imgView.setImageBitmap(bitmap);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 这个方法我们并不应该经常去调用它。因为你完全不需要担心缓存的数据过多从而占用SD卡太多空间的问题，
     * DiskLruCache会根据我们在调用open()方法时设定的缓存最大值来自动删除多余的缓存。
     * 只有你确定某个key对应的缓存内容已经过期，需要从网络获取最新数据的时候才应该调用remove()方法来移除缓存。
     *
     * @param md5Key
     */
    private void removeImg(String md5Key) {
        try {
            mDiskLruCache.remove(md5Key);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 手动将所有的缓存数据全部删除
     */
    private void deleteCache(){
        try {
            //该方法内部调用了DiskLruCache.close()方法。
            mDiskLruCache.delete();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取当前缓存路径下所有缓存数据的总字节数，以byte为单位
     * @return
     */
    private long getCacheSize(){
        long size=mDiskLruCache.size();
        return size;
    }

}

