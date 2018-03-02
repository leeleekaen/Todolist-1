package com.yueyue.todolist.common.utils.entity;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * author : yueyue on 2018/3/3 10:17
 * desc   :
 */

public class PlanTask implements Parcelable{
    public String id;
    public int priority;
    public String title;
    public String describe;
    public long time;
    public int state; //0: normal; 1: finished

    public PlanTask() {
    }

    protected PlanTask(Parcel in) {
        id = in.readString();
        priority = in.readInt();
        title = in.readString();
        describe = in.readString();
        time = in.readLong();
        state = in.readInt();
    }

    public static final Creator<PlanTask> CREATOR = new Creator<PlanTask>() {
        @Override
        public PlanTask createFromParcel(Parcel in) {
            return new PlanTask(in);
        }

        @Override
        public PlanTask[] newArray(int size) {
            return new PlanTask[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeInt(priority);
        dest.writeString(title);
        dest.writeString(describe);
        dest.writeLong(time);
        dest.writeInt(state);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PlanTask planTask = (PlanTask) o;

        if (priority != planTask.priority) return false;
        if (time != planTask.time) return false;
        if (state != planTask.state) return false;
        if (id != null ? !id.equals(planTask.id) : planTask.id != null) return false;
        if (title != null ? !title.equals(planTask.title) : planTask.title != null) return false;
        return describe != null ? describe.equals(planTask.describe) : planTask.describe == null;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + priority;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (describe != null ? describe.hashCode() : 0);
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + state;
        return result;
    }
}