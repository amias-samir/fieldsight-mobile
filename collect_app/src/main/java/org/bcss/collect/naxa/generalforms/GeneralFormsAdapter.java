package org.bcss.collect.naxa.generalforms;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import org.bcss.collect.android.R;
import org.bcss.collect.android.application.Collect;
import org.bcss.collect.naxa.common.Constant;
import org.bcss.collect.naxa.common.OnFormItemClickListener;
import org.bcss.collect.naxa.generalforms.data.GeneralForm;
import org.bcss.collect.naxa.previoussubmission.model.GeneralFormAndSubmission;
import org.bcss.collect.naxa.previoussubmission.model.SubmissionDetail;
import org.odk.collect.android.utilities.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.bcss.collect.naxa.common.AnimationUtils.getRotationAnimation;

public class GeneralFormsAdapter extends RecyclerView.Adapter<GeneralFormsAdapter.ViewHolder> {

    private ArrayList<GeneralFormAndSubmission> generalForms;
    private OnFormItemClickListener<GeneralForm> listener;

    GeneralFormsAdapter(ArrayList<GeneralFormAndSubmission> totalList, OnFormItemClickListener<GeneralForm> listener) {
        this.generalForms = totalList;
        this.listener = listener;
        setHasStableIds(true);
    }

    public void updateList(List<GeneralFormAndSubmission> newList) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new GeneralFormsDiffCallback(newList, generalForms));
        generalForms.clear();
        generalForms.addAll(newList);
        diffResult.dispatchUpdatesTo(this);

        if(newList.isEmpty()){
            //triggers observer so it display empty layout - nishon
            this.notifyDataSetChanged();
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.form_list_item_expanded, null);
        return new ViewHolder(view);


    }

    @Override
    public long getItemId(int position) {
        GeneralForm generalForm = generalForms.get(position).getGeneralForm();
        return Long.parseLong(generalForm.getFsFormId());//fsFormId is always a number
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, int position) {
        GeneralForm generalForm = generalForms.get(viewHolder.getAdapterPosition()).getGeneralForm();
        SubmissionDetail submissionDetail = generalForms.get(viewHolder.getAdapterPosition()).getSubmissionDetail();

        viewHolder.tvFormName.setText(generalForm.getName());

        String relativeDateTime = DateTimeUtils.getRelativeTime(generalForm.getDateCreated(), true);
        viewHolder.tvDesc.setText(viewHolder.tvFormName.getContext().getString(R.string.form_created_on, relativeDateTime));
        if (generalForm.getName() != null) {
            viewHolder.tvIconText.setText(generalForm.getName().substring(0, 1).toUpperCase());
        }
        setSubmissionText(viewHolder, submissionDetail, generalForm);
    }

    private void setSubmissionText(ViewHolder viewHolder, SubmissionDetail submissionDetail, GeneralForm generalForm) {

        String submissionDateTime = "";
        String submittedBy = "";
        String submissionStatus = "";
        String formCreatedOn = "";
        Context context = viewHolder.cardView.getContext();

        if (submissionDetail != null) {
            submittedBy = submissionDetail.getSubmittedBy();
            submissionStatus = submissionDetail.getStatusDisplay();
            submissionDateTime = DateTimeUtils.getRelativeTime(submissionDetail.getSubmissionDateTime(), true);
        }

        if (submissionDetail != null && submissionDetail.getSubmissionDateTime() == null) {
            submissionDateTime = context.getString(R.string.form_pending_submission);
        } else {
            submissionDateTime = context.getString(R.string.form_last_submission_datetime, submissionDateTime);
        }

        if (generalForm.getDateCreated() != null) {
            formCreatedOn = DateTimeUtils.getRelativeTime(generalForm.getDateCreated(), true);
        }

        String formSubtext = generateSubtext(context, submittedBy, submissionStatus, formCreatedOn);

        viewHolder.ivCardCircle.setImageDrawable(getCircleDrawableBackground(submissionStatus));
        viewHolder.tvDesc.setText(submissionDateTime);
        viewHolder.tvSubtext.setText(formSubtext);
    }

    private String generateSubtext(Context context, String submittedBy, String submissionStatus, String formCreatedOn) {
        return context.getString(R.string.form_last_submitted_by, submittedBy == null ? "" : submittedBy)
                + "\n" +
                context.getString(R.string.form_last_submission_status, submissionStatus == null ? "" : submissionStatus)
                + "\n" +
                context.getString(R.string.form_created_on, formCreatedOn == null ? "" : formCreatedOn);
    }


    private Drawable getCircleDrawableBackground(String status) {

        Drawable drawable = ContextCompat.getDrawable(Collect.getInstance().getApplicationContext(), R.drawable.circle_blue);

        if (status == null) return drawable;

        switch (status) {
            case Constant.FormStatus.Approved:
                drawable = ContextCompat.getDrawable(Collect.getInstance().getApplicationContext(), R.drawable.circle_green);
                break;
            case Constant.FormStatus.Flagged:
                drawable = ContextCompat.getDrawable(Collect.getInstance().getApplicationContext(), R.drawable.circle_yellow);
                break;
            case Constant.FormStatus.Rejected:
                drawable = ContextCompat.getDrawable(Collect.getInstance().getApplicationContext(), R.drawable.circle_red);
                break;
            case Constant.FormStatus.Pending:
            default:
                drawable = ContextCompat.getDrawable(Collect.getInstance().getApplicationContext(), R.drawable.circle_blue);
                break;
        }

        return drawable;
    }


    @Override
    public int getItemCount() {
        return generalForms.size();
    }

    public ArrayList<GeneralFormAndSubmission> getAll() {
        return generalForms;
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private TextView tvFormName, tvDesc, tvIconText, tvSubtext;
        private Button btnOpenEdu, btnOpenHistory;
        private ImageView ivCardCircle;
        private CardView cardView;
        private ImageButton btnExpandCard;


        public ViewHolder(View view) {
            super(view);

            cardView = view.findViewById(R.id.card_view_form_list_item);

            tvFormName = view.findViewById(R.id.tv_form_primary);
            tvDesc = view.findViewById(R.id.tv_form_secondary);
            tvIconText = view.findViewById(R.id.form_icon_text);
            tvSubtext = view.findViewById(R.id.tv_form_subtext);

            ivCardCircle = view.findViewById(R.id.iv_form_circle);

            btnOpenHistory = view.findViewById(R.id.btn_form_responses);
            btnOpenEdu = view.findViewById(R.id.btn_form_edu);
            btnExpandCard = view.findViewById(R.id.btn_expand_card);

            cardView.setOnClickListener(this);
            btnOpenEdu.setOnClickListener(this);
            btnOpenHistory.setOnClickListener(this);
            btnExpandCard.setOnClickListener(this);

        }


        @Override
        public void onClick(View v) {
            GeneralForm generalForm = generalForms.get(getAdapterPosition()).getGeneralForm();

            switch (v.getId()) {
                case R.id.btn_form_edu:
                    listener.onGuideBookButtonClicked(generalForm, getAdapterPosition());
                    break;
                case R.id.btn_form_responses:
                    listener.onFormHistoryButtonClicked(generalForm);
                    break;
                case R.id.card_view_form_list_item:
                    listener.onFormItemClicked(generalForm, getAdapterPosition());
                    break;
                case R.id.btn_expand_card:

                    boolean isCollapsed = tvSubtext.getVisibility() == View.GONE;
                    if (isCollapsed) {
                        btnExpandCard.startAnimation(getRotationAnimation(180, 0));
                        btnExpandCard.setRotation(180);
                    } else {
                        btnExpandCard.startAnimation(getRotationAnimation(180, 360));
                        btnExpandCard.setRotation(360);
                    }

                    tvSubtext.setVisibility(tvSubtext.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);

                    break;
            }
        }
    }


}
