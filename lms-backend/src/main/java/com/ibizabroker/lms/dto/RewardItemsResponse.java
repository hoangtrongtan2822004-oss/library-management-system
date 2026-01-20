package com.ibizabroker.lms.dto;

import java.util.List;

public class RewardItemsResponse {
    private List<RewardItemDTO> rewards;
    private Integer userPoints;

    public RewardItemsResponse(List<RewardItemDTO> rewards, Integer userPoints) {
        this.rewards = rewards;
        this.userPoints = userPoints;
    }

    public List<RewardItemDTO> getRewards() { return rewards; }
    public void setRewards(List<RewardItemDTO> rewards) { this.rewards = rewards; }

    public Integer getUserPoints() { return userPoints; }
    public void setUserPoints(Integer userPoints) { this.userPoints = userPoints; }

    public static class RewardItemDTO {
        private Long id;
        private String name;
        private String description;
        private String icon;
        private Integer cost;
        private String category;
        private Boolean available;
        private Boolean canAfford;

        public RewardItemDTO() {}

        public RewardItemDTO(Long id, String name, String description, String icon, Integer cost, 
                            String category, Boolean available, Boolean canAfford) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.icon = icon;
            this.cost = cost;
            this.category = category;
            this.available = available;
            this.canAfford = canAfford;
        }

        // Getters and Setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }

        public Integer getCost() { return cost; }
        public void setCost(Integer cost) { this.cost = cost; }

        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }

        public Boolean getAvailable() { return available; }
        public void setAvailable(Boolean available) { this.available = available; }

        public Boolean getCanAfford() { return canAfford; }
        public void setCanAfford(Boolean canAfford) { this.canAfford = canAfford; }
    }
}
