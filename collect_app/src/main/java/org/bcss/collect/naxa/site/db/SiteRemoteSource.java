package org.bcss.collect.naxa.site.db;

import org.bcss.collect.android.R;
import org.bcss.collect.android.application.Collect;
import org.bcss.collect.naxa.common.BaseRemoteDataSource;
import org.bcss.collect.naxa.common.Constant;
import org.bcss.collect.naxa.common.FieldSightNotificationUtils;
import org.bcss.collect.naxa.common.database.SiteUploadHistory;
import org.bcss.collect.naxa.common.rx.RetrofitException;
import org.bcss.collect.naxa.login.model.Site;
import org.bcss.collect.naxa.network.APIEndpoint;
import org.bcss.collect.naxa.network.ApiInterface;
import org.bcss.collect.naxa.sync.DisposableManager;
import org.bcss.collect.naxa.sync.SyncLocalSource;
import org.bcss.collect.naxa.sync.SyncRepository;
import org.odk.collect.android.dao.InstancesDao;
import org.odk.collect.android.utilities.FileUtils;

import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.observers.DisposableObserver;
import io.reactivex.observers.DisposableSingleObserver;
import io.reactivex.schedulers.Schedulers;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import timber.log.Timber;

import static org.bcss.collect.naxa.common.Constant.DownloadUID.EDITED_SITES;
import static org.bcss.collect.naxa.common.Constant.DownloadUID.EDU_MATERIALS;
import static org.bcss.collect.naxa.common.Constant.DownloadUID.OFFLINE_SITES;

import static org.bcss.collect.naxa.common.Constant.SiteStatus.IS_EDITED;
import static org.bcss.collect.naxa.common.Constant.SiteStatus.IS_OFFLINE;
import static org.bcss.collect.naxa.common.Constant.SiteStatus.IS_ONLINE;
import static org.bcss.collect.naxa.network.ServiceGenerator.getRxClient;

public class SiteRemoteSource implements BaseRemoteDataSource<Site> {

    private static SiteRemoteSource INSTANCE;
    private SiteDao dao;


    public static SiteRemoteSource getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new SiteRemoteSource();
        }
        return INSTANCE;
    }


    @Override
    public void getAll() {


    }


    public void updateAllEditedSite() {

        DisposableSingleObserver<List<Site>> dis = SiteLocalSource.getInstance()
                .getAllByStatus(IS_EDITED)
                .doOnDispose(() -> SyncLocalSource.getINSTANCE().markAsFailed(EDITED_SITES))
                .doOnSubscribe(disposable -> {
                    SyncRepository.getInstance().showProgress(EDITED_SITES);
                    SyncLocalSource.getINSTANCE().markAsRunning(EDITED_SITES);
                })
                .flattenAsObservable((Function<List<Site>, Iterable<Site>>) sites -> sites)
                .flatMap((Function<Site, ObservableSource<Site>>) this::updateSite)
                .map(site -> {
                    SiteLocalSource.getInstance().updateSiteIdAsync(site.getId(), IS_ONLINE);
                    return site;
                })
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSingleObserver<List<Site>>() {
                    @Override
                    public void onSuccess(List<Site> sites) {

                        if (sites.size() > 0) {
                            String title = Collect.getInstance().getString(R.string.msg_edited_site_uploaded);
                            String msg;
                            if (sites.size() > 1) {
                                msg = Collect.getInstance().getString(R.string.msg_multiple_sites_upload, sites.get(0).getName(), sites.size());
                            } else {
                                msg = Collect.getInstance().getString(R.string.msg_single_site_upload, sites.get(0).getName());
                            }
                            FieldSightNotificationUtils.getINSTANCE().notifyHeadsUp(title, msg);
                            SyncLocalSource.getINSTANCE().markAsCompleted(EDITED_SITES);
                        } else {
                            SyncLocalSource.getINSTANCE().markAsFailed(EDITED_SITES);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                        String message;
                        if (e instanceof RetrofitException) {
                            message = ((RetrofitException) e).getKind().getMessage();
                        } else {
                            message = e.getMessage();
                        }

                        SyncLocalSource.getINSTANCE().markAsFailed(EDITED_SITES,message);3
                    }
                });


        DisposableManager.add(dis);
    }

    public void uploadAllOfflineSite() {
        DisposableSingleObserver<List<Site>> dis = SiteLocalSource.getInstance()
                .getAllByStatus(Constant.SiteStatus.IS_OFFLINE)
                .doOnDispose(() -> SyncLocalSource.getINSTANCE().markAsFailed(OFFLINE_SITES))
                .doOnSubscribe(disposable -> {
                    SyncRepository.getInstance().showProgress(OFFLINE_SITES);
                    SyncLocalSource.getINSTANCE().markAsRunning(OFFLINE_SITES);
                })
                .toObservable()
                .flatMap((Function<List<Site>, ObservableSource<Site>>) this::uploadMultipleSites)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .toList()
                .subscribeWith(new DisposableSingleObserver<List<Site>>() {
                    @Override
                    public void onSuccess(List<Site> sites) {
                        SyncLocalSource.getINSTANCE().markAsCompleted(OFFLINE_SITES);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Timber.e(e);
                        String message;
                        if (e instanceof RetrofitException && ((RetrofitException) e).getResponse().errorBody() == null) {
                            message = ((RetrofitException) e).getKind().getMessage();
                        } else {
                            message = e.getMessage();
                        }
                        SyncLocalSource.getINSTANCE().markAsFailed(OFFLINE_SITES, message);
                    }
                });

        DisposableManager.add(dis);
    }


    public Observable<Site> uploadMultipleSites(List<Site> sites) {
        InstancesDao instancesDao = new InstancesDao();
        return Observable.just(sites)
                .flatMapIterable((Function<List<Site>, Iterable<Site>>) sites1 -> sites1)
                .filter(site -> site.getIsSiteVerified() == IS_OFFLINE)
                .flatMap(new Function<Site, Observable<Site>>() {
                    @Override
                    public Observable<Site> apply(Site oldSite) {
                        return uploadSite(oldSite)
                                .flatMap(new Function<Site, ObservableSource<Site>>() {
                                    @Override
                                    public ObservableSource<Site> apply(Site newSite) {
                                        String oldSiteId = oldSite.getId();
                                        String newSiteId = newSite.getId();
                                        return SiteLocalSource.getInstance()
                                                .setSiteAsVerified(oldSiteId)
                                                .flatMap(new Function<Integer, ObservableSource<Integer>>() {
                                                    @Override
                                                    public ObservableSource<Integer> apply(Integer integer) {
                                                        return SiteLocalSource.getInstance().updateSiteId(oldSiteId, newSiteId);
                                                    }
                                                })
                                                .flatMap(new Function<Integer, Observable<Long[]>>() {
                                                    @Override
                                                    public Observable<Long[]> apply(Integer affectedRowsCount) {
                                                        return SiteUploadHistoryLocalSource.getInstance().saveAsObservable(new SiteUploadHistory(newSiteId, oldSiteId));
                                                    }
                                                }).flatMap(new Function<Long[], ObservableSource<Integer>>() {
                                                    @Override
                                                    public ObservableSource<Integer> apply(Long[] updatedRows) {
                                                        return instancesDao.cascadedSiteIds(oldSiteId, newSiteId);
                                                    }
                                                })
                                                .map(new Function<Integer, Site>() {
                                                    @Override
                                                    public Site apply(Integer integer) {
                                                        return newSite;
                                                    }
                                                });
                                    }
                                });
                    }
                });

    }


    private Observable<Site> uploadSite(Site siteLocationPojo) {
        RequestBody requestBody;
        RequestBody siteTypeRequest = null;
        MultipartBody.Part body = null;

        File file = FileUtils.getFileByPath(siteLocationPojo.getLogo());

        if (FileUtils.isFileExists(file)) {
            requestBody = RequestBody.create(MediaType.parse("image/*"), file);
            body = MultipartBody.Part.createFormData("logo", file.getName(), requestBody);
        }

        boolean hasSiteType = siteLocationPojo.getTypeId() != null && siteLocationPojo.getTypeId().trim().length() > 0;

        if (hasSiteType) {
            siteTypeRequest = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(siteLocationPojo.getTypeId()));
        }

        RequestBody SiteNameRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getName());
        RequestBody latRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getLatitude());
        RequestBody lonRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getLongitude());
        RequestBody identifierRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getIdentifier());
        RequestBody SitePhoneRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getPhone());
        RequestBody SiteAddressRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getAddress());
        RequestBody SitePublicDescRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getPublicDesc());
        RequestBody projectIdRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getProject());
        RequestBody isSurvey = RequestBody.create(MediaType.parse("text/plain"), "false");
        RequestBody metaAttrs = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getMetaAttributes());
        RequestBody regionId = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getRegionId());


        return getRxClient()
                .create(ApiInterface.class)
                .uploadSite(APIEndpoint.ADD_SITE_URL, body, isSurvey
                        , SiteNameRequest, latRequest, lonRequest, identifierRequest, SitePhoneRequest,
                        SiteAddressRequest, SitePublicDescRequest, projectIdRequest, siteTypeRequest, regionId, metaAttrs);
    }

    private Observable<Site> updateSite(Site siteLocationPojo) {
        RequestBody requestBody;
        MultipartBody.Part body = null;

        File file = FileUtils.getFileByPath(siteLocationPojo.getLogo());

        if (FileUtils.isFileExists(file)) {
            requestBody = RequestBody.create(MediaType.parse("image/*"), file);
            body = MultipartBody.Part.createFormData("logo", file.getName(), requestBody);
        }

        RequestBody SiteNameRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getName());
        RequestBody latRequest = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(siteLocationPojo.getLatitude()));
        RequestBody lonRequest = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(siteLocationPojo.getLongitude()));
        RequestBody identifierRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getIdentifier());
        RequestBody SitePhoneRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getPhone());
        RequestBody SiteAddressRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getAddress());
        RequestBody SitePublicDescRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getPublicDesc());
        RequestBody projectIdRequest = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getProject());
        RequestBody SiteRequest = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(siteLocationPojo.getTypeId()));
        RequestBody metaAttrs = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getMetaAttributes());
        RequestBody regionId = RequestBody.create(MediaType.parse("text/plain"), siteLocationPojo.getRegionId());


        return getRxClient()
                .create(ApiInterface.class)
                .updateSite(APIEndpoint.SITE_UPDATE_URL.concat(siteLocationPojo.getId()), body
                        , SiteNameRequest, latRequest, lonRequest, identifierRequest, SitePhoneRequest,
                        SiteAddressRequest, SitePublicDescRequest, projectIdRequest, SiteRequest, regionId, metaAttrs);

    }
}
