package rewear.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Entity representing an automated multi-party circular clothing barter (SwapChain).
 * Demonstrates Graph Cycle Results, Collections (List, Map), and Agreement Consensus.
 */
public class SwapChain implements Identifiable {
    private String id;
    private List<String> participantIds;
    private List<String> participantNames;
    private List<String> listingIds;
    private List<String> itemTitles;
    private List<String> itemPhotos;
    private Map<String, String> approvals; // studentId -> "PENDING", "ACCEPTED", "REJECTED"
    private String status; // "PENDING", "COMPLETED", "REJECTED"
    private String createdAt;
    private String description;

    public SwapChain() {
        this.participantIds = new ArrayList<>();
        this.participantNames = new ArrayList<>();
        this.listingIds = new ArrayList<>();
        this.itemTitles = new ArrayList<>();
        this.itemPhotos = new ArrayList<>();
        this.approvals = new HashMap<>();
        this.status = "PENDING";
    }

    public SwapChain(String id, List<String> participantIds, List<String> participantNames,
                     List<String> listingIds, List<String> itemTitles, List<String> itemPhotos,
                     String description, String createdAt) {
        this.id = id;
        this.participantIds = participantIds != null ? new ArrayList<>(participantIds) : new ArrayList<>();
        this.participantNames = participantNames != null ? new ArrayList<>(participantNames) : new ArrayList<>();
        this.listingIds = listingIds != null ? new ArrayList<>(listingIds) : new ArrayList<>();
        this.itemTitles = itemTitles != null ? new ArrayList<>(itemTitles) : new ArrayList<>();
        this.itemPhotos = itemPhotos != null ? new ArrayList<>(itemPhotos) : new ArrayList<>();
        this.approvals = new HashMap<>();
        for (String pid : this.participantIds) {
            this.approvals.put(pid, "PENDING");
        }
        this.description = description;
        this.status = "PENDING";
        this.createdAt = createdAt;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public List<String> getParticipantIds() {
        return participantIds;
    }

    public void setParticipantIds(List<String> participantIds) {
        this.participantIds = participantIds;
    }

    public List<String> getParticipantNames() {
        return participantNames;
    }

    public void setParticipantNames(List<String> participantNames) {
        this.participantNames = participantNames;
    }

    public List<String> getListingIds() {
        return listingIds;
    }

    public void setListingIds(List<String> listingIds) {
        this.listingIds = listingIds;
    }

    public List<String> getItemTitles() {
        return itemTitles;
    }

    public void setItemTitles(List<String> itemTitles) {
        this.itemTitles = itemTitles;
    }

    public List<String> getItemPhotos() {
        return itemPhotos;
    }

    public void setItemPhotos(List<String> itemPhotos) {
        this.itemPhotos = itemPhotos;
    }

    public Map<String, String> getApprovals() {
        return approvals;
    }

    public void setApprovals(Map<String, String> approvals) {
        this.approvals = approvals;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean recordVote(String studentId, boolean accept) {
        if (!participantIds.contains(studentId)) {
            return false;
        }
        if (!accept) {
            approvals.put(studentId, "REJECTED");
            this.status = "REJECTED";
            return false;
        }
        approvals.put(studentId, "ACCEPTED");

        // Check if all accepted
        boolean allAccepted = true;
        for (String pid : participantIds) {
            if (!"ACCEPTED".equalsIgnoreCase(approvals.get(pid))) {
                allAccepted = false;
                break;
            }
        }
        if (allAccepted) {
            this.status = "COMPLETED";
            return true;
        }
        return false;
    }

    public boolean isParticipant(String studentId) {
        return participantIds.contains(studentId);
    }

    public String getVoteStatus(String studentId) {
        return approvals.getOrDefault(studentId, "PENDING");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SwapChain that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}

