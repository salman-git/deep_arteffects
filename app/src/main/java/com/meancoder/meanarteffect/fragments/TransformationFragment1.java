package com.meancoder.meanarteffect.fragments;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.meancoder.meanarteffect.MainViewModel;
import com.meancoder.meanarteffect.R;
import com.meancoder.meanarteffect.databinding.FragmentTransformationBinding;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;


public class TransformationFragment1 extends Fragment {

//    private FragmentTransformationBinding binding;
//    private MainViewModel viewModel;
//    private NavController navController;
//    public TransformationFragment() {
//        // Required empty public constructor
//    }
//
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//
//    }
//
//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container,
//                             Bundle savedInstanceState) {
//        // Inflate the layout for this fragment
//        binding = FragmentTransformationBinding.inflate(inflater, container, false);
//
//        viewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
//
//        NavHostFragment navHostFragment =
//                (NavHostFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
//        navController = navHostFragment.getNavController();
//
//        binding.imageView.setImageBitmap(viewModel.getInputBitmapValue());
//        binding.imageView.setOnTouchListener(new View.OnTouchListener() {
//            @Override
//            public boolean onTouch(View v, MotionEvent event) {
//                switch (event.getAction()) {
//                    case MotionEvent.ACTION_DOWN:
//                        binding.linearLayout.setVisibility(View.GONE);
//                        break;
//                    case MotionEvent.ACTION_UP:
//                        binding.linearLayout.setVisibility(View.VISIBLE);
//                        break;
//                }
//                return true;
//            }
//        });
//
//        return binding.getRoot();
//    }
//    private List<StyleAdapter.Style> getListStyle() {
//        List<StyleAdapter.Style> styles = new ArrayList<>();
//        try {
//            for ( String it : requireActivity().getAssets().list("thumbnails")) {
//                styles.add(new StyleAdapter.Style(it, false));
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return styles;
//    }
//
//    private Bitmap getBitmapFromAssets( String fileName) {
//        AssetManager assetManager = requireActivity().getAssets();
//        try {
//            InputStream istr = assetManager.open(fileName);
//            Bitmap bitmap = BitmapFactory.decodeStream(istr);
//            istr.close();
//            return bitmap;
//        } catch (Exception e) {
//            return null;
//        }
//    }
}