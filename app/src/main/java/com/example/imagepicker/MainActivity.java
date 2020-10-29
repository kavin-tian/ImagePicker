package com.example.imagepicker;

import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.lqr.imagepicker.ImagePicker;
import com.lqr.imagepicker.Utils;
import com.lqr.imagepicker.bean.ImageItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends CheckPermissionsActivity {

    private ImageView imageView;
    private static final int pick_image = 100;
    private static final int take_photo = 101;
    private static final int take_photo_2 = 102;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
    }

    @Override
    protected ArrayList<String> addNeedPermissionList(ArrayList<String> permissions) {
        //permissions.add(Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS);
        permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        permissions.add(Manifest.permission.CAMERA);
        //permissions.add(Manifest.permission.RECORD_AUDIO);
        return permissions;
    }

    private static final int REQUEST_CODE_PICK_IMAGE = 100;

    /**
     * 选择照片 单选
     */
    public void updatePortrait(View view) {
        ImagePicker.picker().pick(this, REQUEST_CODE_PICK_IMAGE);
    }

    /**
     * 选择照片 多选
     */
    public void pick_image(View containerView) {
        Intent intent = ImagePicker.picker().showCamera(true).enableMultiMode(9).buildPickIntent(this);
//        Intent intent = ImagePicker.picker().showCamera(true).enableMultiMode(1).buildPickIntent(this);
        startActivityForResult(intent, pick_image);

    }

    //拍照完成图片保存的路径
//    String imagePath = Environment.getExternalStorageDirectory().getPath() + File.separator + "my.png";
    String imagePath;

    /**
     * 拍照
     */
    public void take_photo(View view) {
        imagePath = Utils.genTakePhotoOutputPath();//调用一次会产生一个不同的path
        takePhoto(this, imagePath, take_photo);
    }

    /**
     * 拍照的方法
     */
    public static void takePhoto(Activity activity, String outputPath, int requestCode) {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        takePictureIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (takePictureIntent.resolveActivity(activity.getPackageManager()) != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(activity, activity.getPackageName() + ".provider", new File(outputPath)));
        }
        activity.startActivityForResult(takePictureIntent, requestCode);
    }

    /**
     * 拍照
     * 权限:　Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA
     */
    public void take_photo_2(View view) {
        Intent intent = new Intent(this, TakePhotoActivity.class);
        startActivityForResult(intent, take_photo_2);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case pick_image:
                    pick_image(data);
                    break;
                case take_photo:
                    take_photo(data);
                    break;
                case take_photo_2:
                    take_photo_2(data);
                    break;

            }
        }
    }

    private void take_photo(Intent data) {
        //发送广播通知图片增加了
        Utils.notifyToScanMedia(this, imagePath);
        //大图需要进行压缩
//        File imageFileSource = ImageUtils.compressImage(this, imagePath);
//        Bitmap bitmap = BitmapFactory.decodeFile(imageFileSource.getAbsolutePath());
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);
        imageView.setImageBitmap(bitmap);
    }


    private void take_photo_2(Intent data) {
        if (data == null) {
            return;
        }

        String path = data.getStringExtra("path");
        if (TextUtils.isEmpty(path)) {
            Toast.makeText(this, "拍照错误, 请向我们反馈", Toast.LENGTH_SHORT).show();
            return;
        }


        //File imageFileSource = new File(path);
        //大图需要进行压缩
        File imageFileSource = ImageUtils.compressImage(this, path);
        Bitmap bitmap = BitmapFactory.decodeFile(imageFileSource.getAbsolutePath());
        imageView.setImageBitmap(bitmap);

       /* if (data.getBooleanExtra("take_photo", true)) {
            //照片
            messageViewModel.sendImgMsg(conversation, ImageUtils.genThumbImgFile(path), new File(path));
        } else {
            //小视频
            messageViewModel.sendVideoMsg(conversation, new File(path));
        }*/
    }

    private void pick_image(Intent data) {

        if (data == null) {
            return;
        }

        //是否发送原图
        boolean compress = data.getBooleanExtra(ImagePicker.EXTRA_COMPRESS, true);

        ArrayList<ImageItem> images = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);

        //可以返还多张图片，需要时可以处理
        for (ImageItem imageItem : images) {
            boolean isGif = isGifFile(imageItem.path);
            if (isGif) {
                //
                Toast.makeText(this, "请不要选择GIF动图...", Toast.LENGTH_LONG).show();
                continue;
            }
            File imageFileSource;
            // FIXME: 2018/11/29 压缩, 不是发原图的时候，大图需要进行压缩
            if (compress) {
                //大图需要进行压缩
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