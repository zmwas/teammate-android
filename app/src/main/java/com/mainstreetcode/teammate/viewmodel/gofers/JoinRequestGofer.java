package com.mainstreetcode.teammate.viewmodel.gofers;

import android.support.annotation.IntDef;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;

import com.mainstreetcode.teammate.R;
import com.mainstreetcode.teammate.model.Identifiable;
import com.mainstreetcode.teammate.model.Item;
import com.mainstreetcode.teammate.model.JoinRequest;
import com.mainstreetcode.teammate.model.TeamMember;
import com.mainstreetcode.teammate.repository.TeamMemberRepository;

import java.lang.annotation.Retention;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

import static io.reactivex.android.schedulers.AndroidSchedulers.mainThread;
import static java.lang.annotation.RetentionPolicy.SOURCE;

public class JoinRequestGofer extends TeamHostingGofer<JoinRequest> {

    @Retention(SOURCE)
    @IntDef({INVITING, JOINING, APPROVING, ACCEPTING, WAITING})
    public @interface JoinRequestState {}

    public static final int INVITING = 0;
    public static final int JOINING = 1;
    public static final int APPROVING = 2;
    public static final int ACCEPTING = 3;
    public static final int WAITING = 4;

    private int state;
    private int index;
    private final List<Item<JoinRequest>> items;

    private final Function<JoinRequest, Flowable<JoinRequest>> getFunction;
    private final BiFunction<JoinRequest, Boolean, Single<JoinRequest>> joinCompleter;

    public JoinRequestGofer(JoinRequest model,
                            Consumer<Throwable> onError,
                            Function<JoinRequest, Flowable<JoinRequest>> getFunction,
                            BiFunction<JoinRequest, Boolean, Single<JoinRequest>> joinCompleter) {
        super(model, onError);
        this.getFunction = getFunction;
        this.joinCompleter = joinCompleter;
        index = getIndex(model);
        updateState();
        items = new ArrayList<>(filteredItems(model));
    }

    public boolean showsFab() {
        switch (state) {
            case INVITING:
            case JOINING:
                return true;
            case APPROVING:
                return hasPrivilegedRole();
            case ACCEPTING:
                return isRequestOwner();
            default:
                return false;
        }
    }

    public boolean canEditFields() {
        return state == INVITING;
    }

    public boolean canEditRole() {
        return state == INVITING || state == JOINING;
    }

    public boolean isRequestOwner() {
        return getSignedInUser().equals(model.getUser());
    }

    private void updateState() {
        boolean isEmpty = model.isEmpty();
        boolean isRequestOwner = isRequestOwner();
        boolean isUserEmpty = model.getUser().isEmpty();
        boolean isUserApproved = model.isUserApproved();
        boolean isTeamApproved = model.isTeamApproved();

        state = isEmpty && isUserEmpty && isTeamApproved
                ? INVITING
                : isEmpty && isUserApproved && isRequestOwner
                ? JOINING
                : (!isEmpty && isUserApproved && isRequestOwner) || (!isEmpty && isTeamApproved && !isRequestOwner)
                ? WAITING
                : isTeamApproved && isRequestOwner ? ACCEPTING : APPROVING;
    }

    @JoinRequestState
    public int getState() {
        return state;
    }

    public String getToolbarTitle(Fragment fragment) {
        return fragment.getString(state == JOINING
                ? R.string.join_team
                : state == INVITING
                ? R.string.invite_user
                : state == WAITING
                ? R.string.pending_request
                : state == APPROVING ? R.string.approve_request : R.string.accept_request);
    }

    @StringRes
    public int getFabTitle() {
        switch (state) {
            case JOINING:
                return R.string.join_team;
            case INVITING:
                return R.string.invite;
            case APPROVING:
                return R.string.approve;
            default:
            case WAITING:
            case ACCEPTING:
                return R.string.accept;
        }
    }

    @Nullable
    @Override
    public String getImageClickMessage(Fragment fragment) {
        return fragment.getString(R.string.no_permission);
    }

    @Override
    public Flowable<DiffUtil.DiffResult> fetch() {
        Flowable<List<Item<JoinRequest>>> source = Flowable.defer(() -> getFunction.apply(model)).map(JoinRequest::asItems);
        return Identifiable.diff(source, this::getItems, (items, updated) -> filteredItems(model));
    }

    @Override
    Single<DiffUtil.DiffResult> upsert() {
        Single<JoinRequest> single = model.isEmpty() ? joinTeam() : approveRequest();
        Single<List<Item<JoinRequest>>> source = single.map(JoinRequest::asItems).doOnSuccess(ignored -> updateState());
        return Identifiable.diff(source, this::getItems, (items, updated) -> filteredItems(model));
    }

    @Override
    public Completable delete() {
        return Single.defer(() -> joinCompleter.apply(model, false)).toCompletable().observeOn(mainThread());
    }

    private Single<JoinRequest> joinTeam() {
        TeamMember<JoinRequest> member = TeamMember.fromModel(model);
        TeamMemberRepository<JoinRequest> repository = TeamMemberRepository.getInstance();

        return repository.createOrUpdate(member).map(ignored -> model);
    }

    private Single<JoinRequest> approveRequest() {
        return Single.defer(() -> joinCompleter.apply(model, true));
    }

    public List<Item<JoinRequest>> getItems() {
        return items;
    }

    private boolean filter(Item<JoinRequest> item) {
        boolean isEmpty = model.isEmpty();
        int sortPosition = item.getSortPosition();

        if (item.getItemType() == Item.ROLE) return true;

        // Joining a team
        boolean joining = state == JOINING || state == ACCEPTING || (state == WAITING && isRequestOwner());
        if (joining) return sortPosition > index;

        // Inviting a user
        boolean ignoreTeam = sortPosition <= index;

        int stringRes = item.getStringRes();

        // About field should not show when inviting a user, email field should not show when trying
        // to join a team.
        return isEmpty
                ? ignoreTeam && stringRes != R.string.user_about
                : ignoreTeam && stringRes != R.string.email;
    }

    private Integer getIndex(JoinRequest model) {
        return Flowable.range(0, model.asItems().size() - 1)
                .filter(index -> model.asItems().get(index).getItemType() == Item.ROLE)
                .first(0)
                .blockingGet();
    }

    private List<Item<JoinRequest>> filteredItems(JoinRequest request) {
        return Flowable.fromIterable(request.asItems())
                .filter(this::filter)
                .collect((Callable<ArrayList<Item<JoinRequest>>>) ArrayList::new, List::add)
                .blockingGet();
    }
}