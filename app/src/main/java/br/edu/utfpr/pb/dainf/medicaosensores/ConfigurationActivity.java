package br.edu.utfpr.pb.dainf.medicaosensores;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import br.edu.utfpr.pb.dainf.medicaosensores.level.Level;
import br.edu.utfpr.pb.dainf.medicaosensores.model.Configuration;

public class ConfigurationActivity extends AppCompatActivity {

    TextView edtMarcaSmart;
    TextView edtModeloSmart;
    TextView edtVeiculo;
    TextView edtTaxaGPS;
    TextView edtTaxaAcell;
    TextView edtKmVeiculo;
    TextView edtMontagemSmart;

    ArrayAdapter<String> adapterAcelerometro;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_configuration);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        this.edtMarcaSmart = (TextView) findViewById(R.id.edtMarcaSmart);
        this.edtModeloSmart = (TextView) findViewById(R.id.edtModeloSmart);
        this.edtVeiculo = (TextView) findViewById(R.id.edtVeiculo);
        this.edtTaxaGPS = (TextView) findViewById(R.id.edtTaxaGPS);
        this.edtTaxaAcell = (TextView) findViewById(R.id.edtTaxaAcell);
        this.edtKmVeiculo = (TextView) findViewById(R.id.edtKmVeiculo);
        this.edtMontagemSmart = (TextView) findViewById(R.id.edtMontagemSmart);

        try {
            Gson gson = new Gson();
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            Configuration  config = gson.fromJson( preferences.getString("config", "") , Configuration.class);
            edtMarcaSmart.setText( ((config.getMarca()!= null && !config.getMarca().equals("")) ? config.getMarca() : Build.MANUFACTURER) );
            edtModeloSmart.setText( ((config.getModelo()!= null && !config.getModelo().equals("")) ? config.getModelo() : Build.MODEL));
            edtVeiculo.setText(config.getVeiculo());
            edtTaxaGPS.setText((isNumeric(config.getTaxaGPS()) ? config.getTaxaGPS() : "1000") );
            edtTaxaAcell.setText((isNumeric(config.getTaxaAcelerometro()) ? config.getTaxaAcelerometro() : "0") );
            edtMontagemSmart.setText(config.getTipoMontagem());
            edtKmVeiculo.setText(config.getKmVeiculo());
            //int position = adapterAcelerometro.getPosition(config.getTaxaAcelerometro());
            //spiTaxaAcell.setSelection(position);


        }catch (Exception ex){
            edtMarcaSmart.setText( Build.MANUFACTURER );
            edtModeloSmart.setText( Build.MODEL);
            edtVeiculo.setText("");
            edtTaxaGPS.setText("1000");
            edtTaxaAcell.setText("0");
            edtMontagemSmart.setText("");
            edtKmVeiculo.setText("");
            ex.printStackTrace();
        }

        Button btnSalvar = (Button) findViewById(R.id.btnSalvar);
        btnSalvar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gravarDados();
                showMessage("Configurações salvas com sucesso!");
            }
        });


        Button btnTelaMedicao = (Button) findViewById(R.id.btnTelaMedicao);
        btnTelaMedicao.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if ( gravarDados() ) {
                    abrirTelaMedicao();
                }
            }
        });

        Button btnLevelBolha = (Button) findViewById(R.id.btnLevelBolha);
        btnLevelBolha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                abrirTelaBolha();
            }
        });
    }

    private void showMessage(String mensagem){
        Toast.makeText(this, mensagem, Toast.LENGTH_SHORT).show();
    }

    public boolean isNumeric(String s) {
        return s != null && s.matches("[-+]?\\d*\\.?\\d+");
    }

    private void carregarSpinner(){
        try {
            List<String> lista = new ArrayList<>();
            lista.add("SENSOR_DELAY_GAME");
            lista.add("SENSOR_DELAY_NORMAL");
            lista.add("SENSOR_DELAY_UI");
            lista.add("SENSOR_DELAY_FASTEST");

            adapterAcelerometro = new ArrayAdapter<String>(this,
                    android.R.layout.simple_list_item_1, lista );
            //spiTaxaAcell.setAdapter(adapterAcelerometro);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }


    private boolean gravarDados() {
        try {
            Configuration config = new Configuration();
            config.setMarca(edtMarcaSmart.getText().toString());
            config.setModelo(edtModeloSmart.getText().toString());
            config.setVeiculo(edtVeiculo.getText().toString());
            config.setTaxaGPS(edtTaxaGPS.getText().toString());
            //config.setTaxaAcelerometro( (String) spiTaxaAcell.getSelectedItem());
            config.setTaxaAcelerometro(edtTaxaAcell.getText().toString());
            config.setKmVeiculo(edtKmVeiculo.getText().toString());
            config.setTipoMontagem(edtMontagemSmart.getText().toString());

            Gson gson = new Gson();

            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString("config", gson.toJson(config));
            editor.apply();
            return true;
        }catch (Exception ex){
            Toast.makeText(this, "Falha ao gravar configurações. " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            ex.printStackTrace();
            return false;
        }
    }

    public void abrirTelaMedicao(){
        //Intent intent = new Intent(this, MainActivity.class);
        Intent intent = new Intent(this, Test.class);
        startActivity(intent);
    }

    public void abrirTelaBolha(){
        //Intent intent = new Intent(this, MainActivity.class);
        Intent intent = new Intent(this, Level.class);
        startActivity(intent);
    }
}
