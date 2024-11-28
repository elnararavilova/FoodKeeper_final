package com.example.foodkeeper_final.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.navigation.fragment.findNavController
import com.example.foodkeeper_final.R

class ProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        
        // Настройка навигации для кнопок
        view.findViewById<Button>(R.id.btnSettings)?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_settings)
        }
        
        view.findViewById<Button>(R.id.btnAccount)?.setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_account)
        }

        
        return view
    }

}