package hn.uth.minicrmapp.ui.crm;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import hn.uth.minicrmapp.database.Cliente;
import hn.uth.minicrmapp.databinding.FragmentClientesBinding;
import hn.uth.minicrmapp.entity.Contacto;

public class ClientesFragment extends Fragment {

    private static final int PERMISSION_REQUEST_READ_CONTACT = 700;
    private FragmentClientesBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        ClientesViewModel clientesViewModel =
                new ViewModelProvider(this).get(ClientesViewModel.class);

        binding = FragmentClientesBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

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
            clientesViewModel.insert(nuevo);
            Snackbar.make(binding.getRoot(), "Cliente agregado correctamente", Snackbar.LENGTH_LONG).show();
            limpiarCampos();
        });

        return root;
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