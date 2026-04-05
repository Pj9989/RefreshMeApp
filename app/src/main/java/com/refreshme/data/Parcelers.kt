package com.refreshme.data

import android.os.Parcel
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import kotlinx.parcelize.Parceler
import java.util.Date

object GeoPointParceler : Parceler<GeoPoint?> {
    override fun create(parcel: Parcel): GeoPoint? {
        val lat = parcel.readDouble()
        val lon = parcel.readDouble()
        return if (lat == -1000.0 && lon == -1000.0) null else GeoPoint(lat, lon)
    }

    override fun GeoPoint?.write(parcel: Parcel, flags: Int) {
        if (this == null) {
            parcel.writeDouble(-1000.0)
            parcel.writeDouble(-1000.0)
        } else {
            parcel.writeDouble(latitude)
            parcel.writeDouble(longitude)
        }
    }
}

object DateParceler : Parceler<Date?> {
    override fun create(parcel: Parcel): Date? {
        val time = parcel.readLong()
        return if (time == -1L) null else Date(time)
    }

    override fun Date?.write(parcel: Parcel, flags: Int) {
        parcel.writeLong(this?.time ?: -1L)
    }
}

object TimestampParceler : Parceler<Timestamp?> {
    override fun create(parcel: Parcel): Timestamp? {
        val seconds = parcel.readLong()
        val nanos = parcel.readInt()
        return if (seconds == -1L) null else Timestamp(seconds, nanos)
    }

    override fun Timestamp?.write(parcel: Parcel, flags: Int) {
        if (this == null) {
            parcel.writeLong(-1L)
            parcel.writeInt(0)
        } else {
            parcel.writeLong(seconds)
            parcel.writeInt(nanoseconds)
        }
    }
}