package com.durgesh.service;

public class test {

    public static int[] twoSum(int[] nums, int target) {
        int re[]=new int[2];

        for(int i=0;i<nums.length;i++)
        {
            if(nums[i]+nums[i+1]==target)
            {
                re[0]=nums[i];
                re[1]=nums[i+1];
                break ;
            }
        }
        return re;
    }






    public static void main(String[] args) {
        int arr[]= {2,7,11,15};
        twoSum(arr,9);
    }
}
