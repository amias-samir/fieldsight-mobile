package org.odk.collect.naxa.scheduled.data;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

import org.odk.collect.naxa.generalforms.data.GeneralForm;
import org.odk.collect.naxa.login.model.Site;

import java.util.List;

import static org.odk.collect.naxa.common.Constant.FormDeploymentFrom.PROJECT;
import static org.odk.collect.naxa.common.Constant.FormDeploymentFrom.SITE;

public class ScheduledFormViewModel extends ViewModel {
    private final ScheduledFormRepository repository;

    public ScheduledFormViewModel(ScheduledFormRepository repository) {
        this.repository = repository;
    }

    public LiveData<List<ScheduleForm>> getForms(boolean forcedUpdate, Site loadedSite) {
        switch (loadedSite.getGeneralFormDeployedFrom()) {
            case SITE:
                return repository.getBySiteId(forcedUpdate, loadedSite.getId());
            case PROJECT:
            default:
                return repository.getByProjectId(forcedUpdate, loadedSite.getProject());

        }
    }
}