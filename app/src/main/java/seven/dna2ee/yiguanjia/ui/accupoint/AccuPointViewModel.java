package seven.dna2ee.yiguanjia.ui.accupoint;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;

import seven.dna2ee.yiguanjia.util.StringEx;

public class AccuPointViewModel extends ViewModel {

    private MutableLiveData<String> textRecords;
    private MutableLiveData<String> textCurrent;

    private ArrayList<String> records;
    private String msg;
    private double accupoint;

    public AccuPointViewModel() {
        this.accupoint = -1;
        this.records = new ArrayList<>();
        this.textRecords = new MutableLiveData<>();
        this.textCurrent = new MutableLiveData<>();
        this.msg = "...";
        this.clrRecord();
    }

    public void addRecord(String record) {
        records.add(record);
        textRecords.setValue(StringEx.join("\n", records));
    }

    public void clrRecord() {
        records.clear();
        textRecords.setValue("");
    }

    public ArrayList<String> getRawRecords() {
        return this.records;
    }

    public void setAccupoint(double val) {
        this.accupoint = val;
    }

    public void setMsg(String _msg) {
        this.msg = _msg;
        textCurrent.setValue(this.msg);
    }

    public void addAccupointRecord(String name) {
        if (this.accupoint < 0) {
            return;
        }
        String record = String.format("%s: %.2f", name, this.accupoint);
        this.addRecord(record);
    }

    public LiveData<String> getRecords() { return textRecords; }

    public LiveData<String> getCurrent() { return textCurrent; }
}