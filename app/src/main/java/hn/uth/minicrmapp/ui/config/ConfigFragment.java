package hn.uth.minicrmapp.ui.config;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import hn.uth.minicrmapp.database.Cliente;
import hn.uth.minicrmapp.databinding.FragmentConfigBinding;

public class ConfigFragment extends Fragment {

    private FragmentConfigBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ConfigViewModel notificationsViewModel =
                new ViewModelProvider(this).get(ConfigViewModel.class);

        binding = FragmentConfigBinding.inflate(inflater, container, false);
        View root = binding.getRoot();
        parseImage(getArguments());


        return root;
    }

    private void parseImage(Bundle bundleArgs) {
        if(bundleArgs != null){
            try{
                Bundle bundle = bundleArgs.getBundle("foto");
                if(bundle != null){
                    Bitmap imagen = (Bitmap) bundle.get("data");
                    binding.imgFull.setImageBitmap(imagen);
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            try{
                String rutaFoto = bundleArgs.getString("rutafoto");
                if(rutaFoto != null){
                    showFullImage(rutaFoto);
                }

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }



    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    private void showFullImage(String ubicacionFotoTomada)  {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int screenWidth = DeviceDimensionsHelper.getDisplayWidth(this.getContext());

        bmOptions.inJustDecodeBounds = false;
        bmOptions.outWidth = screenWidth;
        bmOptions.inPurgeable = true;

        Bitmap image = BitmapFactory.decodeFile(ubicacionFotoTomada, bmOptions);
        Bitmap rotatedBitmap = null;
        try{
            ExifInterface ei = new ExifInterface(ubicacionFotoTomada);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch(orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotatedBitmap = rotateImage(image, 90);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotatedBitmap = rotateImage(image, 180);
                    break;

                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotatedBitmap = rotateImage(image, 270);
                    break;

                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    rotatedBitmap = image;
            }
        }catch(Exception e){
            rotatedBitmap = image;
            e.printStackTrace();
        }


        binding.imgFull.setImageBitmap(rotatedBitmap);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
    public static class DeviceDimensionsHelper {
        // DeviceDimensionsHelper.getDisplayWidth(context) => (display width in pixels)
        public static int getDisplayWidth(Context context) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            return displayMetrics.widthPixels;
        }

        // DeviceDimensionsHelper.getDisplayHeight(context) => (display height in pixels)
        public static int getDisplayHeight(Context context) {
            DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
            return displayMetrics.heightPixels;
        }

        // DeviceDimensionsHelper.convertDpToPixel(25f, context) => (25dp converted to pixels)
        public static float convertDpToPixel(float dp, Context context) {
            Resources r = context.getResources();
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
        }

        // DeviceDimensionsHelper.convertPixelsToDp(25f, context) => (25px converted to dp)
        public static float convertPixelsToDp(float px, Context context) {
            Resources r = context.getResources();
            DisplayMetrics metrics = r.getDisplayMetrics();
            float dp = px / (metrics.densityDpi / 160f);
            return dp;
        }

    }
}