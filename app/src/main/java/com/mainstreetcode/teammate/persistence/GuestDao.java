package com.mainstreetcode.teammate.persistence;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import com.mainstreetcode.teammate.model.Guest;
import com.mainstreetcode.teammate.persistence.entity.GuestEntity;

import java.util.Date;
import java.util.List;

import io.reactivex.Maybe;

@Dao
public abstract class GuestDao extends EntityDao<GuestEntity> {

    @Override
    protected String getTableName() {
        return "guests";
    }

    @Query("SELECT * FROM guests" +
            " WHERE :id = guest_id")
    public abstract Maybe<Guest> get(String id);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    public abstract void insert(List<GuestEntity> guests);

    @Update(onConflict = OnConflictStrategy.IGNORE)
    protected abstract void update(List<GuestEntity> guests);

    @Delete
    public abstract void delete(List<GuestEntity> guests);

    @Query("DELETE FROM guests " +
            " WHERE guest_user = :userId" +
            " AND guest_event IN (" +
            " SELECT event_id FROM events event " +
            " INNER JOIN teams team" +
            " ON (event.event_team = team.team_id)" +
            " WHERE team.team_id = :teamId" +
            ")")
    public abstract void deleteUsers(String userId, String teamId);

    @Query("SELECT * FROM guests" +
            " WHERE :eventId = guest_event" +
            " AND guest_created < :date" +
            " ORDER BY guest_created DESC" +
            " LIMIT :limit")
    public abstract Maybe<List<Guest>> getGuests(String eventId, Date date, int limit);

    @Query("SELECT * FROM guests" +
            " WHERE :userId = guest_user" +
            " AND guest_created < :date" +
            " AND guest_attending = 1" +
            " ORDER BY guest_created DESC" +
            " LIMIT 40")
    public abstract Maybe<List<Guest>> getRsvpList(String userId, Date date);
}