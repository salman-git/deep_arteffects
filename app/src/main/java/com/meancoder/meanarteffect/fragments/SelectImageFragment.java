package com.meancoder.meanarteffect.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;

import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.meancoder.meanarteffect.MainViewModel;
import com.meancoder.meanarteffect.R;
import com.meancoder.meanarteffect.databinding.FragmentSelectImageBinding;

import java.io.IOException;


public class SelectImageFragment extends Fragment {
    private FragmentSelectImageBinding binding;
    private MainViewModel viewModel;
    private NavController navController;

    public SelectImageFragment() {
        // Required empty public constructor
    }


    public static SelectImageFragment newInstance(String param1, String param2) {
        SelectImageFragment fragment = new SelectImageFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSelectImageBinding.inflate(inflater,container,false);
        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);


        return binding.getRoot();
    }
}