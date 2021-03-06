package org.bcss.collect.naxa.substages;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
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
import org.bcss.collect.naxa.previoussubmission.model.SubStageAndSubmission;
import org.bcss.collect.naxa.previoussubmission.model.SubmissionDetail;
import org.bcss.collect.naxa.stages.data.SubStage;
import org.odk.collect.android.utilities.DateTimeUtils;

import java.util.ArrayList;
import java.util.List;

import static org.bcss.collect.naxa.common.AnimationUtils.getRotationAnimation;

/**
 * Created by Susan on 1/30/2017.
 */
public class SubStageListAdapter extends
        RecyclerView.Adapter<SubStageListAdapter.ViewHolder> {


    private List<SubStageAndSubmission> subStages;
    private OnFormItemClickListener<SubStage> listener;
    private String stageOrder;


    public SubStageListAdapter(List<SubStageAndSubmission> subStages, String stageOrder, OnFormItemClickListener<SubStage> listener) {
        this.subStages = subStages;
        this.stageOrder = stageOrder;
        this.listener = listener;
    }

    public void updateList(List<SubStageAndSubmission> newList) {

        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new SubStageDiffCallback(newList, subStages));
        subStages.clear();
        subStages.addAll(newList);
        diffResult.dispatchUpdatesTo(this);

        if (newList.isEmpty()) {
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


    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, int position) {
        SubStage subStage = subStages.get(viewHolder.getAdapterPosition()).getSubStage();
        SubmissionDetail submissionDetail = subStages.get(viewHolder.getAdapterPosition()).getSubmissionDetail();

        viewHolder.tvFormName.setText(subStage.getName());
        viewHolder.tvDesc.setText(subStage.getName());

        setSubstageNumber(viewHolder);
        setSubmissionText(viewHolder, submissionDetail, subStage);


    }

    private void setSubstageNumber(ViewHolder viewHolder) {

        int substageNumber = viewHolder.getAdapterPosition() + 1;
        int stageNumber = Integer.parseInt(stageOrder) + 1;
        String iconText = stageNumber + "." + substageNumber;

        viewHolder.tvIconText.setText(iconText);
    }

    private void setSubmissionText(SubStageListAdapter.ViewHolder viewHolder, SubmissionDetail submissionDetail, SubStage subStage) {

        String submissionDateTime = "";
        String submittedBy = "";
        String submissionStatus = "";
        String subStageDesc = "";
        String formCreatedAt = "";
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

        if (subStage.getDescription() != null) {
            subStageDesc = subStage.getDescription();
        }

        String formSubtext = generateSubtext(context, submittedBy, submissionStatus, subStageDesc);


        viewHolder.ivCardCircle.setImageDrawable(getCircleDrawableBackground(submissionStatus));
        viewHolder.tvDesc.setText(submissionDateTime);
        viewHolder.tvSubtext.setText(formSubtext);
    }

    private String generateSubtext(Context context, String submittedBy, String submissionStatus, String subStageDesc) {
        return context.getString(R.string.form_last_submitted_by, submittedBy == null ? "" : submittedBy)
                + "\n" +
                context.getString(R.string.form_last_submission_status, submissionStatus == null ? "" : submissionStatus)
                + "\n" +
                context.getString(R.string.substage_description, subStageDesc == null ? "" : subStageDesc);
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
        return subStages.size();
    }

    public ArrayList<SubStageAndSubmission> getAll() {
        return (ArrayList<SubStageAndSubmission>) subStages;
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

            SubStage subStage = subStages.get(getAdapterPosition()).getSubStage();

            switch (v.getId()) {
                case R.id.btn_form_edu:
                    listener.onGuideBookButtonClicked(subStage, getAdapterPosition());
                    break;
                case R.id.btn_form_responses:
                    listener.onFormHistoryButtonClicked(subStage);
                    break;
                case R.id.card_view_form_list_item:
                    listener.onFormItemClicked(subStage, getAdapterPosition());
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

                    int newVisiblity = tvSubtext.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
                    tvSubtext.setVisibility(newVisiblity);
                    break;
            }


        }

        private void setSubmissionText(SubStageListAdapter.ViewHolder viewHolder, SubmissionDetail submissionDetail, String formCreatedAt) {

            String submissionDateTime = "";
            String submittedBy = "";
            String submissionStatus = "";
            Context context = viewHolder.cardView.getContext();

            if (submissionDetail == null && !TextUtils.isEmpty(formCreatedAt)) {
                viewHolder.tvSubtext.setText(
                        context.getString
                                (R.string.form_created_on,
                                        DateTimeUtils.getRelativeTime(formCreatedAt, true)
                                ));
                viewHolder.tvDesc.setText(R.string.form_pending_submission);
                return;
            }

            if (submissionDetail == null) {
                viewHolder.tvDesc.setText(R.string.form_pending_submission);
                return;
            }

            submittedBy = submissionDetail.getSubmittedBy();
            submissionStatus = submissionDetail.getStatusDisplay();
            submissionDateTime = DateTimeUtils.getRelativeTime(submissionDetail.getSubmissionDateTime(), true);


            String formSubtext = context.getString(R.string.form_last_submitted_by, submittedBy)
                    + "\n" +
                    context.getString(R.string.form_last_submission_status, submissionStatus);


            viewHolder.ivCardCircle.setImageDrawable(getCircleDrawableBackground(submissionDetail.getStatusDisplay()));
            viewHolder.tvDesc.setText(context.getString(R.string.form_last_submission_datetime, submissionDateTime));
            viewHolder.tvSubtext.setText(formSubtext);
        }

        private Drawable getCircleDrawableBackground(String status) {

            Drawable drawable;
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


    }


}



