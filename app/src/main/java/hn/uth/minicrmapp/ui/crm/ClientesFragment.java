package hn.uth.minicrmapp.ui.crm;

import static android.app.Activity.RESULT_OK;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import hn.uth.minicrmapp.R;
import hn.uth.minicrmapp.database.Cliente;
import hn.uth.minicrmapp.databinding.FragmentClientesBinding;
import hn.uth.minicrmapp.entity.Contacto;

public class ClientesFragment extends Fragment {

    private static final int PERMISSION_REQUEST_READ_CONTACT = 700;
    private static final int REQUEST_IMAGE_CAPTURE = 600;
    private FragmentClientesBinding binding;
    private Cliente clienteEditar;
    private Intent intentFoto;
    private String ubicacionFotoTomada;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ClientesViewModel clientesViewModel = new ViewModelProvider(this).get(ClientesViewModel.class);
        ubicacionFotoTomada = null;
        intentFoto = null;
        binding = FragmentClientesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        try{
            clienteEditar = (Cliente) getArguments().getSerializable("cliente");
        }catch (Exception e){
            clienteEditar = null;
        }
        mostrarClienteEditar();

        binding.btnSearch.setOnClickListener(v -> {

            if(binding.tilSearch.getEditText() != null && binding.tilSearch.getEditText().getText() != null && !binding.tilSearch.getEditText().toString().isEmpty()){
                String busqueda = binding.tilSearch.getEditText().getText().toString();
                if(busqueda.length() >= 4){
                    List<Contacto> contactos = solicitarPermisoContactos(this.getContext());
                    mostrarContacto(contactos, false, true);
                }else{
                    Snackbar.make(binding.getRoot(), "Debes de escribir un nombre de mínimo 4 caracteres", Snackbar.LENGTH_LONG).show();
                }
            }else{
                Snackbar.make(binding.getRoot(), "Debes de escribir un nombre para realizar la búsqueda", Snackbar.LENGTH_LONG).show();
            }

        });

        binding.btnGuardar.setOnClickListener(v -> {

            Cliente nuevo = new Cliente(binding.tilNombre.getEditText().getText().toString(),
                    binding.tilTelefono.getEditText().getText().toString(),
                    binding.tilCorreo.getEditText().getText().toString());
            String mensaje = "Cliente agregado correctamente";
            if(clienteEditar == null){
                clientesViewModel.insert(nuevo);
            }else{
                nuevo.setIdCliente(clienteEditar.getIdCliente());
                clientesViewModel.update(nuevo);
                mensaje = "Cliente modificado correctamente";
            }

            Snackbar.make(binding.getRoot(), mensaje, Snackbar.LENGTH_LONG).show();
            limpiarCampos();
            NavController navController = Navigation.findNavController(this.getActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_home);
        });

        binding.btnEliminar.setOnClickListener(v -> {
            clientesViewModel.delete(clienteEditar);
            Snackbar.make(binding.getRoot(), "Cliente eliminado correctamente", Snackbar.LENGTH_LONG).show();
            NavController navController = Navigation.findNavController(this.getActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_home);
        } );

        binding.imgFoto.setOnClickListener(v -> showImage());
        binding.btnTomarFoto.setOnClickListener( v-> abrirCamaraFotos());

        return root;
    }

    private void abrirCamaraFotos() {
        Intent tomarFotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if(tomarFotoIntent.resolveActivity(this.getActivity().getPackageManager()) != null){
            File photoFile = null;
            try{
                photoFile = createImageFile();
            }catch(IOException e){
                e.printStackTrace();
                Toast.makeText(this.getContext(), "No se pudo crear el archivo de la fotografia", Toast.LENGTH_LONG).show();
            }
            if(photoFile != null){
                Uri photoUri = FileProvider.getUriForFile(this.getContext(), "hn.uth.minicrmapp.fileprovider",photoFile);
                tomarFotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(tomarFotoIntent, REQUEST_IMAGE_CAPTURE);
            }
        }
    }

    private File createImageFile() throws IOException {
        String actualtime = new SimpleDateFormat("yyMMdd_HHmmss").format(new Date());
        String imageName = "JPEG_"+actualtime+"_";
        File storageDir = this.getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageName, ".jpg",storageDir);
        ubicacionFotoTomada = image.getAbsolutePath();

        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //OBTENIENDO LA VISTA PREVIA DE LA FOTOGRAFIA (IMAGEN PEQUEÑA)
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            if(data == null){
                showFullImage();
            }else{
                Bundle extra = data.getExtras();
                Bitmap imagen = (Bitmap) extra.get("data");
                binding.imgFoto.setImageBitmap(imagen);
                intentFoto = data;
            }
        }
    }

    private void showFullImage()  {
        int targetW = binding.imgFoto.getWidth();
        int targetH = binding.imgFoto.getHeight();

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;

        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;

        int scaleFactor = Math.min(photoW / targetW, photoH/targetH);

        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
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


        binding.imgFoto.setImageBitmap(rotatedBitmap);

    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                matrix, true);
    }

    public void showImage(){
        Bundle bundle = new Bundle();
        if(intentFoto != null) {
            bundle.putBundle("foto", intentFoto.getExtras());
            NavController navController = Navigation.findNavController(this.getActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_config, bundle);
        }else if(ubicacionFotoTomada != null){
            bundle.putString("rutafoto", ubicacionFotoTomada);
            NavController navController = Navigation.findNavController(this.getActivity(), R.id.nav_host_fragment_activity_main);
            navController.navigate(R.id.navigation_config, bundle);
        }else{
            Toast.makeText(this.getContext(), "No hay ninguna foto configurada", Toast.LENGTH_LONG).show();
        }
    }

    private void mostrarClienteEditar() {
        if(clienteEditar == null){
            //ES UNA CREACIÓN, MOSTRAR BUSQUEDA
            binding.cvBuscar.setVisibility(View.VISIBLE);
            binding.btnGuardar.setText(R.string.btn_crear_cliente);
        }else{
            //ES UNA EDICIO, OCULTAR BUSQUEDA
            binding.cvBuscar.setVisibility(View.GONE);
            binding.tilNombre.getEditText().setText(clienteEditar.getNombre());
            binding.tilTelefono.getEditText().setText(clienteEditar.getTelefono());
            binding.tilCorreo.getEditText().setText(clienteEditar.getCorreo());
            binding.btnGuardar.setText(R.string.btn_modificar_cliente);
            binding.btnEliminar.setVisibility(View.VISIBLE);
        }
    }

    private void limpiarCampos() {
        binding.tilNombre.getEditText().setText("");
        binding.tilTelefono.getEditText().setText("");
        binding.tilCorreo.getEditText().setText("");
        binding.tilSearch.getEditText().setText("");
    }

    private List<Contacto> solicitarPermisoContactos(Context context){
        //PREGUNTANDO SI YA TENGO UN DETERMINADO PERMISO
        if(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            //ENTRA AQUI SI NO ME HAN DADO EL PERMISO, Y DEBO DE SOLICITARLO
            ActivityCompat.requestPermissions(this.getActivity(), new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACT);
            return null;
        }else{
            //ENTRA AQUI SI EL USUARIO YA ME OTORGÓ EL PERMISO ANTES, PUEDO HACER USO DE LA LECTURA DE CONTACTOS
            return getContacts(context);
        }
    }

    private List<Contacto> getContacts(Context context) {
        List<Contacto> contactos = new ArrayList<>();

        String buscar = binding.tilSearch.getEditText().getText().toString();


        ContentResolver resolver = context.getContentResolver();
        Cursor cursor = resolver.query(ContactsContract.Contacts.CONTENT_URI,
                null, ContactsContract.Contacts.DISPLAY_NAME + " LIKE '"+buscar+"%'", null, ContactsContract.Contacts.DISPLAY_NAME + " DESC");
        boolean continuar = true;
        if(cursor.getCount() > 0){
            while (continuar && cursor.moveToNext()){
                int idColumnIndex = Math.max(cursor.getColumnIndex(ContactsContract.Contacts._ID), 0);
                int nameColumnIndex = Math.max(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME), 0);
                int phoneColumnIndex = Math.max(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER), 0);//ME DICE SI TIENE O NO UN TELEFONO GUARDADO

                String id = cursor.getString(idColumnIndex);
                String name = cursor.getString(nameColumnIndex);

                if(Integer.parseInt(cursor.getString(phoneColumnIndex)) > 0){
                    //EL CONTACTO SI TIENE TELEFONO ALMACENADO
                    Cursor cursorPhone = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + "=?", new String[]{id}, null);

                    Cursor cursorCorreo = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                                    null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?", new String[]{id}, null);


                    while (continuar && cursorPhone.moveToNext()){
                        cursorCorreo.moveToNext();
                        int phoneCommonColumIndex = cursorPhone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        String phone = cursorPhone.getString(phoneCommonColumIndex);

                        String correo = "";
                        if(cursorCorreo.getCount()>0){
                            int emailCommonColumIndex = cursorCorreo.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA1);
                            correo = cursorCorreo.getString(emailCommonColumIndex);
                        }

                        Contacto nuevo = new Contacto();
                        nuevo.setName(name);
                        nuevo.setPhone(phone);
                        nuevo.setEmail(correo); //ME FALTA BUSCAR ESTE

                        contactos.add(nuevo);

                        continuar = false;
                    }

                    //Cursor cursorCorreo = resolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                    //        null, ContactsContract.CommonDataKinds.Email.CONTACT_ID + "=?", new String[]{id}, null);

                    /*while (cursorCorreo.moveToNext()){
                        int emailCommonColumIndex = cursorCorreo.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA1);
                        String correo = cursorCorreo.getString(emailCommonColumIndex);

                        Contacto nuevo = new Contacto();
                        nuevo.setName(name);
                        nuevo.setPhone(correo);
                        //nuevo.setEmail(""); ME FALTA BUSCAR ESTE

                        contactos.add(nuevo);
                    }*/
                    cursorPhone.close();
                }
            }
            cursor.close();
        }

        return contactos;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //AQUI VA A ENTRAR CUANDO EL USUARIO INDIQUE SI ME OTORGÓ O NO EL PERMISO
        if(requestCode == PERMISSION_REQUEST_READ_CONTACT){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                //PERMISO EN TIEMPO DE EJECUCIÓN CONCEDIDO, PUEDO USAR LA FUNCIONALIDAD
                List<Contacto> contactos = getContacts(this.getContext());
                mostrarContacto(contactos, true, true);

            }else{
                //PERMISO EN TIEMPO DE EJECUCIÓN NO OTORGADO, MOSTRANDO ALTERNATIVA
                Snackbar.make(binding.getRoot(), "No se pueden buscar contactos", Snackbar.LENGTH_LONG).show();
                binding.tilSearch.getEditText().setText("");
                binding.tilSearch.getEditText().setEnabled(Boolean.FALSE);
            }
        }


        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void mostrarContacto(List<Contacto> contactos, boolean mostrarMensajeExito, boolean MostrarMensajeError) {
        if (contactos == null) {
            Snackbar.make(binding.getRoot(), "No es posible acceder a los contactos", Snackbar.LENGTH_LONG).show();
        }else if(contactos.isEmpty()){
            if(MostrarMensajeError){
                Snackbar.make(binding.getRoot(), "No hay coincidencias en la búsqueda", Snackbar.LENGTH_LONG).show();
            }
        }else{
            binding.tilNombre.getEditText().setText(contactos.get(0).getName());
            binding.tilTelefono.getEditText().setText(contactos.get(0).getPhone());
            binding.tilCorreo.getEditText().setText(contactos.get(0).getEmail());
            Snackbar.make(binding.getRoot(), contactos.size() + " Contactos encontrados", Snackbar.LENGTH_LONG).show();
            if(mostrarMensajeExito){
                Snackbar.make(binding.getRoot(), "Contacto cargado", Snackbar.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}