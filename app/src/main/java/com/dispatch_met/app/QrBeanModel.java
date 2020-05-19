package com.dispatch_met.app;

public class QrBeanModel  {
    private String QrText;
    private String Date;
    private String CheckedStatus;
    public String SpecDate;

    public QrBeanModel(String qrText, String date, String specDate, String checkedStatus) {
        QrText = qrText;
        Date = date;
        SpecDate = specDate;
        CheckedStatus = checkedStatus;
    }

    public String getSpecDate() {
        return SpecDate;
    }

    public void setSpecDate(String specDate) {
        SpecDate = specDate;
    }

    public String getQrText() {
        return QrText;
    }

    public void setQrText(String qrText) {
        QrText = qrText;
    }

    public String getDate() {
        return Date;
    }

    public void setDate(String date) {
        Date = date;
    }

    public String getCheckedStatus() { return CheckedStatus; }

    public void setCheckedStatus(String status) {
        CheckedStatus = status;
    }


}
