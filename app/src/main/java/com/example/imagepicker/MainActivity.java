package com.example.imagepicker;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
    }

    public void pickImage(View containerView) {
        Intent intent = ImagePicker.picker().showCamera(true).enableMultiMode(9).buildPickIntent(this);
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            if (data != null) {

                //是否发送原图
                boolean compress = data.getBooleanExtra(ImagePicker.EXTRA_COMPRESS, true);

                ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);

                //可以返还多张图片，需要时可以处理
                for (ImageItem imageItem : images) {
                    boolean isGif = isGifFile(imageItem.path);
                    if (isGif) {
                        //
                        continue;
                    }
                    File imageFileSource;
                    // FIXME: 2018/11/29 压缩, 不是发原图的时候，大图需要进行压缩
                    if (compress) {
                        imageFileSource = ImageUtils.compressImage(this, imageItem.path);
                    } else {
                        imageFileSource = new File(imageItem.path);
                    }

                    Bitmap bitmap = BitmapFactory.decodeFile(imageFileSource.getAbsolutePath());
                    imageView.setImageBitmap(bitmap);

                    //对比选择压缩前后大小
                    //long length = imageFileSource.length();
                    //Log.e("------TAG", "length: " + length);


                }


            }
        }
    }


    private boolean isGifFile(String file) {
        try {
            FileInputStream inputStream = new FileInputStream(file);
            int[] flags = new int[5];
            flags[0] = inputStream.read();
            flags[1] = inputStream.read();
            flags[2] = inputStream.read();
            flags[3] = inputStream.read();
            inputStream.skip(inputStream.available() - 1);
            flags[4] = inputStream.read();
            inputStream.close();
            return flags[0] == 71 && flags[1] == 73 && flags[2] == 70 && flags[3] == 56 && flags[4] == 0x3B;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }
}