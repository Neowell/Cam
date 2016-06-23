package ru.geomath.camera;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageSwitcher;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.ViewSwitcher;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

@SuppressWarnings("ALL")
public class MainActivity extends AppCompatActivity {

    //интерфейс фотографии и предпросмотра
    RelativeLayout contaner_makephoto;
    RelativeLayout contaner_presave;

    //для вопсроизведения анимации смены камеры
    Animation anim_recam;
    ImageView iv_recam;

    //для генерации пути к создаваемому файлу
    File directory;
    final int TYPE_PHOTO = 1;
    final int TYPE_VIDEO = 2;
    final int REQUEST_CODE_PHOTO = 1;
    ImageView ivPhoto;

    //image switcher для смены режима вспышки
    ImageSwitcher imageSwitcher;
    Animation slide_in_top, slide_out_down;
    int imageResources[] = {
            R.drawable.flash_auto,
            R.drawable.flash_on,
            R.drawable.flash_off,};
    int curIndex;

    //intent
    Intent intent;
    byte[] pictureData;
    private static final int PHOTO_INTENT_REQUEST_CODE = 100;
    private Context context;

    //surface view для вывода на экран
    SurfaceView surfaceView;
    Camera camera;
    File photofile;
    File videofile;
    final int CAMERA_ID = 0;
    final boolean FULL_SCREEN = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        intent = getIntent();
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        //отключение показа верхней системной панели
        decorView.setSystemUiVisibility(uiOptions);
        //только портреная ориентация
        setRequestedOrientation (ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);
        //imageSwitcher
        imageSwitcher = (ImageSwitcher) findViewById(R.id.imageSwitcher);
        slide_in_top = AnimationUtils.loadAnimation(this, R.anim.anim_slide_in_bottom);
        slide_out_down = AnimationUtils.loadAnimation(this, R.anim.anim_slide_out_down);
        imageSwitcher.setInAnimation(slide_in_top);
        imageSwitcher.setOutAnimation(slide_out_down);
        imageSwitcher.setFactory(new ViewSwitcher.ViewFactory() {
            @Override
            public View makeView() {
                ImageView imageView = new ImageView(MainActivity.this);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);

                ViewGroup.LayoutParams params = new ImageSwitcher.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

                imageView.setLayoutParams(params);
                return imageView;
            }
        });
        curIndex = 0;
        imageSwitcher.setImageResource(imageResources[curIndex]);
        imageSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                if (curIndex == imageResources.length - 1) {
                    curIndex = 0;
                    imageSwitcher.setImageResource(imageResources[curIndex]);
                } else {
                    imageSwitcher.setImageResource(imageResources[++curIndex]);
                }

            }
        });


        // Режимы вспышки
        // получаем список режимов вспышки
        if (curIndex == imageResources.length - 1) {
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            camera.setParameters(params);
        } else if (curIndex == imageResources.length) {
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            camera.setParameters(params);
        } else if (curIndex == imageResources.length + 1) {
            Camera.Parameters params = camera.getParameters();
            params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
            camera.setParameters(params);
        }

        iv_recam = (ImageView)findViewById(R.id.recam);
        anim_recam = AnimationUtils.loadAnimation(this, R.anim.anim_recam);

        iv_recam.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iv_recam.startAnimation(anim_recam);
            }
        });

        surfaceView = (SurfaceView) findViewById(R.id.surfaceView);
        SurfaceHolder holder = surfaceView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                try {
                    camera.setPreviewDisplay(holder);
                    camera.startPreview();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                camera.stopPreview();
                setCameraDisplayOrientation(90);
                contaner_makephoto = (RelativeLayout) findViewById(R.id.container_makephoto);
                contaner_presave = (RelativeLayout) findViewById(R.id.container_presave);
                if (contaner_presave.getVisibility() == View.VISIBLE) {
                    new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    intent.getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE);
                } else {
                    try {
                        camera.setPreviewDisplay(holder);
                        camera.startPreview();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open();
        setPreviewSize(FULL_SCREEN);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (camera != null)
            camera.release();
        camera = null;
    }

    //по клику на кнопку получаем изображение и переходим к окну выбора сохранения или отмены
    public void onClickGetPhoto(View view) {
        camera.takePicture(null, null, null, cPictureCallback);
    }
    private Camera.PictureCallback cPictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE);
            pictureData = data;
            showConfirming();
            }
    };

    //не нравится фото, не сохранять
    public void onClickCancel (View view) {
        hideConfirming();
        camera.startPreview();
        pictureData = null;
    }

    //сохранить фотографию
    public void onClickDone (View view) {
        Uri pictureFile;
        if (intent.hasExtra(MediaStore.EXTRA_OUTPUT))
            pictureFile = (Uri)intent.getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
        else pictureFile = generateFileUri();
        try {
            saveImgFile (pictureData, pictureFile);
            intent.putExtra("data", pictureData);
            intent.setData(pictureFile);
            setResult(RESULT_OK, intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
        camera.startPreview();
        hideConfirming();
        pictureData = null;
    }

    //создание пути файла и индексаци для того чтобы он отображался в галерее
    private Uri generateFileUri() {
        File path = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "Camera");
        if (! path.exists()){
            if (! path.mkdirs()){
                return null;
            }
        }
        String timeSys = String.valueOf(System.currentTimeMillis());
        File newFile = new File(path.getPath() + File.separator + timeSys + ".jpg");
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + newFile.getAbsolutePath())));
        return Uri.fromFile(newFile);
    }

    //запись полученного фото
    private void saveImgFile(byte[] data, Uri pictureFile) throws Exception {
        if (pictureFile == null)
            throw new Exception();
            OutputStream os = getContentResolver().openOutputStream(pictureFile);
            os.write(data);
            os.close();
    }

    //подтверждение сохранения
    private void showConfirming() {
        contaner_makephoto = (RelativeLayout) findViewById(R.id.container_makephoto);
        contaner_presave = (RelativeLayout) findViewById(R.id.container_presave);
        contaner_makephoto.setVisibility(View.INVISIBLE);
        contaner_presave.setVisibility(View.VISIBLE);
    }
    private void hideConfirming() {
        contaner_makephoto = (RelativeLayout) findViewById(R.id.container_makephoto);
        contaner_presave = (RelativeLayout) findViewById(R.id.container_presave);
        contaner_presave.setVisibility(View.INVISIBLE);
        contaner_makephoto.setVisibility(View.VISIBLE);
    }

    //не доделал, не успел, должен был менять камеру по клику на фронтальную и обратно
    public void onClickRecam(View view) {
    }

    void setPreviewSize(boolean fullscreen) {
        Display display = getWindowManager().getDefaultDisplay();
        boolean widthIsMax = display.getWidth() > display.getHeight();

        Size size = camera.getParameters().getPreviewSize();

        RectF rectDisplay = new RectF();
        RectF rectPreview = new RectF();
        rectDisplay.set(0, 0, display.getWidth(), display.getHeight());
        if (widthIsMax) {
            rectPreview.set(0, 0, size.width, size.height);
        } else {
            rectPreview.set(0, 0, size.height, size.width);
        }
        Matrix matrix  = new Matrix();
        if (!fullscreen) {
            matrix.setRectToRect(rectPreview, rectDisplay, Matrix.ScaleToFit.START);
            matrix.invert(matrix);
        }
        matrix.mapRect(rectPreview);
        surfaceView.getLayoutParams().height = (int) (rectPreview.bottom);
        surfaceView.getLayoutParams().width = (int) (rectPreview.right);
        matrix.setRotate(90);
    }

    void setCameraDisplayOrientation(int cameraId) {
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        camera.setDisplayOrientation(90);
        Camera.Parameters params = camera.getParameters();
        params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
    }
}
