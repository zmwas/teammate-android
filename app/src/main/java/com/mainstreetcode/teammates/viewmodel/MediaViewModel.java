package com.mainstreetcode.teammates.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.support.v7.util.DiffUtil;

import com.mainstreetcode.teammates.model.Identifiable;
import com.mainstreetcode.teammates.model.Media;
import com.mainstreetcode.teammates.model.Team;
import com.mainstreetcode.teammates.repository.MediaRepository;
import com.mainstreetcode.teammates.util.ModelUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.reactivex.Flowable;

public class MediaViewModel extends ViewModel {

    private final MediaRepository repository;
    private final Map<Team, List<Media>> mediaMap = new HashMap<>();

    public MediaViewModel() {
        repository = MediaRepository.getInstance();
    }

    public List<Media> getMediaList(Team team) {
        List<Media> media = mediaMap.get(team);

        if (media == null) {
            media = new ArrayList<>();
            mediaMap.put(team, media);
        }

        return media;
    }

    public Flowable<Media> getMedia(Media model) {
        return repository.get(model).doOnError(throwable -> ModelUtils.checkForInvalidObject(throwable, model, getMediaList(model.getTeam())));
    }

    public Flowable<DiffUtil.DiffResult> getTeamMedia(List<Media> source, Team team, Date date) {
        return Identifiable.diff(repository.getTeamMedia(team, date), () -> source, ModelUtils::preserveList);
    }

}
