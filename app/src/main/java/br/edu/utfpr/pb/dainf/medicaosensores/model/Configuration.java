package br.edu.utfpr.pb.dainf.medicaosensores.model;

import java.io.Serializable;

public class Configuration implements Serializable{
    private static final long serialVersionUID = 1L;

    private String marca;
    private String modelo;
    private String veiculo;
    private String taxaAcelerometro;
    private String taxaGPS;
    private String taxaGiroscopio;
    private String tipoMontagem;
    private String kmVeiculo;

    public Configuration() {
    }

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }

    public String getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(String veiculo) {
        this.veiculo = veiculo;
    }

    public String getTaxaAcelerometro() {
        return taxaAcelerometro;
    }

    public int getTaxaAcelerometroValue() {
        switch (this.taxaAcelerometro){
            case "SENSOR_DELAY_FASTEST":
                return 0;
            case "SENSOR_DELAY_GAME":
                return 1;
            case "SENSOR_DELAY_UI":
                return 2;
            case "SENSOR_DELAY_NORMAL":
                return 3;
            default:
                return 0;
        }
    }
    public static final int SENSOR_DELAY_FASTEST = 0;
    public static final int SENSOR_DELAY_GAME = 1;
    public static final int SENSOR_DELAY_NORMAL = 3;
    public static final int SENSOR_DELAY_UI = 2;

    public void setTaxaAcelerometro(String taxaAcelerometro) {
        this.taxaAcelerometro = taxaAcelerometro;
    }

    public String getTaxaGPS() {
        return taxaGPS;
    }

    public void setTaxaGPS(String taxaGPS) {
        this.taxaGPS = taxaGPS;
    }

    public String getTaxaGiroscopio() {
        return taxaGiroscopio;
    }

    public void setTaxaGiroscopio(String taxaGiroscopio) {
        this.taxaGiroscopio = taxaGiroscopio;
    }

    public String getTipoMontagem() {
        return tipoMontagem;
    }

    public void setTipoMontagem(String tipoMontagem) {
        this.tipoMontagem = tipoMontagem;
    }

    public String getKmVeiculo() {
        return kmVeiculo;
    }

    public void setKmVeiculo(String kmVeiculo) {
        this.kmVeiculo = kmVeiculo;
    }
}
