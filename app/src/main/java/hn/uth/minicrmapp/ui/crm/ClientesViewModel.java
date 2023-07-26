package hn.uth.minicrmapp.ui.crm;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import hn.uth.minicrmapp.database.Cliente;
import hn.uth.minicrmapp.database.ClientesRepository;

public class ClientesViewModel extends AndroidViewModel {

    private ClientesRepository repository;

    public ClientesViewModel(@NonNull Application app) {
        super(app);
        this.repository = new ClientesRepository(app);
    }
    public ClientesRepository getRepository() {
        return repository;
    }

    public void insert(Cliente nuevo){
        repository.insert(nuevo);
    }

    public void update(Cliente actualizar){
        repository.update(actualizar);
    }

}