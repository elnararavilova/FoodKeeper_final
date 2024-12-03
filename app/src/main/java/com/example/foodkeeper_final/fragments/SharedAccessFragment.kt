package com.example.foodkeeper_final.fragments

import android.app.AlertDialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.foodkeeper_final.MainActivity
import com.example.foodkeeper_final.R
import com.example.foodkeeper_final.databinding.FragmentSharedAccessBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.shape.CornerFamily
import com.google.android.material.shape.ShapeAppearanceModel

class SharedAccessFragment : Fragment() {
    private var _binding: FragmentSharedAccessBinding? = null
    private val binding get() = _binding!!
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSharedAccessBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAccessMode()
    }

    private fun checkAccessMode() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем, совпадает ли CURRENT_USER_ID с ID текущего пользователя
        if (MainActivity.CURRENT_USER_ID == currentUser.uid) {
            // Пользователь в своем личном списке
            setupOwnerUI()
        } else {
            // Пользователь в чужом семейном списке
            setupFamilyMemberUI()
        }
    }

    private fun setupOwnerUI() {
        // Показываем все элементы управления для владельца
        binding.addFamilyButton.visibility = View.VISIBLE
        binding.removeFamilyButton.visibility = View.VISIBLE
        binding.familyMemberInfo.visibility = View.GONE

        // Устанавливаем email текущего пользователя
        binding.currentUserEmail.text = auth.currentUser?.email ?: ""

        // Загружаем членов семьи
        loadFamilyMembers()

        binding.addFamilyButton.setOnClickListener {
            showAddFamilyMemberDialog()
        }

        binding.removeFamilyButton.setOnClickListener {
            showRemoveFamilyMemberDialog()
        }
    }

    private fun loadFamilyMembers() {
        val currentUser = auth.currentUser ?: return
        val familyRef = database.reference.child("Users").child(currentUser.uid).child("family")
        
        familyRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                try {
                    binding.familyMembersContainer.removeAllViews()
                    
                    for (memberSnapshot in snapshot.children) {
                        val memberEmail = memberSnapshot.child("email").getValue(String::class.java)
                        if (memberEmail != null) {
                            addFamilyMemberIcon(memberEmail)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Ошибка при обновлении списка: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Ошибка загрузки членов семьи: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addFamilyMemberIcon(email: String) {
        try {
            // Создаем контейнер для иконки и email
            val container = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(8.dpToPx(), 8.dpToPx(), 8.dpToPx(), 8.dpToPx())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = 8.dpToPx() // Добавляем отступ между иконками
                }
            }

            // Создаем иконку пользователя
            val icon = ShapeableImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(64.dpToPx(), 64.dpToPx())
                setBackgroundColor(resources.getColor(R.color.accent_color, null))
                setImageResource(R.drawable.ic_profile)
                imageTintList = ColorStateList.valueOf(Color.WHITE)
                setPadding(12.dpToPx(), 12.dpToPx(), 12.dpToPx(), 12.dpToPx())
                shapeAppearanceModel = ShapeAppearanceModel()
                    .toBuilder()
                    .setAllCorners(CornerFamily.ROUNDED, 50f)
                    .build()
            }

            // Создаем текст с email
            val emailText = TextView(requireContext()).apply {
                text = email
                textSize = 12f
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = 4.dpToPx()
                    // Ограничиваем ширину текста
                    width = 80.dpToPx()
                }
            }

            // Добавляем элементы в контейнер
            container.addView(icon)
            container.addView(emailText)

            // Добавляем контейнер в список членов семьи в UI потоке
            requireActivity().runOnUiThread {
                binding.familyMembersContainer.addView(container)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка при добавлении иконки: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun Int.dpToPx(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }

    private fun setupFamilyMemberUI() {
        // Скрываем элементы управления и показываем информацию о семье
        binding.addFamilyButton.visibility = View.GONE
        binding.removeFamilyButton.text = "Выйти из семьи"
        binding.familyMemberInfo.visibility = View.VISIBLE

        // Получаем информацию о владельце семьи
        val ownerRef = database.reference.child("Users").child(MainActivity.CURRENT_USER_ID!!)
        ownerRef.child("email").get().addOnSuccessListener { snapshot ->
            val ownerEmail = snapshot.getValue(String::class.java)
            binding.familyMemberInfo.text = "Вы находитесь в семейном списке пользователя: $ownerEmail"
        }

        binding.removeFamilyButton.setOnClickListener {
            showLeaveConfirmationDialog()
        }
    }

    private fun showLeaveConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Подтверждение")
            .setMessage("Вы уверены, что хотите выйти из семейного списка?")
            .setPositiveButton("Да") { _, _ ->
                leaveFamily()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun leaveFamily() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
            return
        }

        // Находим владельца семьи, где текущий пользователь является членом
        val usersRef = database.reference.child("Users")
        usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (userSnapshot in snapshot.children) {
                    val familySnapshot = userSnapshot.child("family")
                    for (familyMember in familySnapshot.children) {
                        val memberEmail = familyMember.child("email").getValue(String::class.java)
                        if (memberEmail == currentUser.email) {
                            // Удаляем пользователя из семьи
                            familyMember.ref.removeValue()
                                .addOnSuccessListener {
                                    // Переключаемся на личный список
                                    MainActivity.CURRENT_USER_ID = currentUser.uid
                                    requireActivity().recreate()
                                    Toast.makeText(context, "Вы вышли из семейного списка", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            return
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showAddFamilyMemberDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_add_family_member, null)
        val emailInput = dialogView.findViewById<EditText>(R.id.emailInput)

        builder.setView(dialogView)
            .setTitle("Добавить члена семьи")
            .setPositiveButton("Добавить") { _, _ ->
                val email = emailInput.text.toString().trim()
                if (email.isNotEmpty()) {
                    addFamilyMember(email)
                } else {
                    Toast.makeText(context, "Введите email", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun addFamilyMember(email: String) {
        try {
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Toast.makeText(context, "Ошибка: пользователь не авторизован", Toast.LENGTH_SHORT).show()
                return
            }

            if (email == currentUser.email) {
                Toast.makeText(context, "Нельзя добавить себя в семью", Toast.LENGTH_SHORT).show()
                return
            }

            // Проверяем, существует ли пользователь с таким email
            val usersRef = database.reference.child("Users")
            usersRef.orderByChild("email").equalTo(email)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            // Пользователь найден, добавляем его в семью
                            var userId: String? = null
                            for (userSnapshot in snapshot.children) {
                                userId = userSnapshot.key
                                break
                            }

                            if (userId != null) {
                                // Проверяем, не добавлен ли уже этот пользователь
                                val familyRef = database.reference
                                    .child("Users")
                                    .child(currentUser.uid)
                                    .child("family")

                                familyRef.orderByChild("email").equalTo(email)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(familySnapshot: DataSnapshot) {
                                            if (familySnapshot.exists()) {
                                                Toast.makeText(context, "Этот пользователь уже в вашей семье", Toast.LENGTH_SHORT).show()
                                            } else {
                                                // Добавляем пользователя в семью
                                                val familyMember = HashMap<String, Any>()
                                                familyMember["userId"] = userId
                                                familyMember["email"] = email

                                                familyRef.push().setValue(familyMember)
                                                    .addOnSuccessListener {
                                                        Toast.makeText(context, "Пользователь добавлен в семью", Toast.LENGTH_SHORT).show()
                                                    }
                                                    .addOnFailureListener { e ->
                                                        Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
                                                    }
                                            }
                                        }

                                        override fun onCancelled(error: DatabaseError) {
                                            Toast.makeText(context, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    })
                            }
                        } else {
                            Toast.makeText(context, "Пользователь с таким email не найден", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(context, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } catch (e: Exception) {
            Toast.makeText(context, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showRemoveFamilyMemberDialog() {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(context, "Необходимо авторизоваться", Toast.LENGTH_SHORT).show()
            return
        }

        val familyRef = database.reference.child("Users").child(currentUserId).child("family")
        
        familyRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val familyMembers = mutableListOf<Pair<String, String>>() // Pair of userId and email

                for (memberSnapshot in snapshot.children) {
                    val email = memberSnapshot.child("email").getValue(String::class.java)
                    val userId = memberSnapshot.child("userId").getValue(String::class.java)
                    if (email != null && userId != null) {
                        familyMembers.add(Pair(userId, email))
                    }
                }

                if (familyMembers.isEmpty()) {
                    Toast.makeText(context, "Список членов семьи пуст", Toast.LENGTH_SHORT).show()
                    return
                }

                val builder = AlertDialog.Builder(requireContext())
                val emailList = familyMembers.map { it.second }.toTypedArray()

                builder.setTitle("Выберите члена семьи для удаления")
                    .setItems(emailList) { dialog, which ->
                        val selectedMember = familyMembers[which]
                        removeFamilyMember(selectedMember.first)
                    }
                    .setNegativeButton("Отмена") { dialog, _ ->
                        dialog.cancel()
                    }

                builder.create().show()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Ошибка: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun removeFamilyMember(userId: String) {
        val currentUserId = auth.currentUser?.uid
        if (currentUserId == null) {
            Toast.makeText(context, "Необходимо авторизоваться", Toast.LENGTH_SHORT).show()
            return
        }

        val familyRef = database.reference.child("Users").child(currentUserId).child("family")
        
        familyRef.child(userId).removeValue()
            .addOnSuccessListener {
                Toast.makeText(context, "Член семьи успешно удален", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Ошибка при удалении: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
