package dor.only.dorking.android.fbprofiles;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by Yoni on 10/24/2016.
 */

public class ParceableIds implements Parcelable {
    private ArrayList<String> theList;

    public ArrayList<String> getTheList() {
        return theList;
    }

    public void setTheList(ArrayList<String> theList) {
        this.theList = theList;
    }

    /**
     * Constructs a Question from values
     */
    public ParceableIds(ArrayList<String> theList) {
        this.theList = theList;
    }

    /**
     * Constructs a ParceableIds from a Parcel
     * @param parcel Source Parcel
     */
    public ParceableIds (Parcel parcel) {
        int theSize=parcel.readInt();
        theList=new ArrayList<>();
        for(int i=0; i<theSize; ++i){
        theList.add(parcel.readString());
        }
    }



    @Override
    public int describeContents() {
        return 0;
    }

    // Required method to write to Parcel
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(theList.size());
        for(int i=0; i<theList.size(); ++i){
        dest.writeString(theList.get(i));
        }
    }


    // Method to recreate a Question from a Parcel
    public static Creator<ParceableIds> CREATOR = new Creator<ParceableIds>() {

        @Override
        public ParceableIds createFromParcel(Parcel source) {
            return new ParceableIds(source);
        }

        @Override
        public ParceableIds[] newArray(int size) {
            return new ParceableIds[size];
        }

    };
}
