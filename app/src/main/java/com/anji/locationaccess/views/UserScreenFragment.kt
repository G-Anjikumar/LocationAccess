package com.anji.locationaccess.views

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.anji.location_sdk.viewmodel.MapViewModel
import com.anji.locationaccess.R
import com.anji.locationaccess.adapter.HorizontalImageViewAdapter
import com.anji.locationaccess.data.model.ImageDetails
import com.anji.locationaccess.data.model.UserDetails
import com.anji.locationaccess.data.model.UserDetailsWithImages
import com.anji.locationaccess.data.viewmodel.UserDataViewModel
import com.anji.locationaccess.databinding.UserScreenLayoutBinding
import com.anji.locationaccess.service.LocationWorker
import com.anji.locationaccess.util.AppConstants
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.gson.Gson
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import javax.inject.Inject
import kotlin.properties.Delegates

@AndroidEntryPoint
class UserScreenFragment : Fragment() {

    private lateinit var userScreenLayoutBinding: UserScreenLayoutBinding
    private val mapViewModel: MapViewModel by activityViewModels()
    private val userViewModel: UserDataViewModel by viewModels<UserDataViewModel>()
    private var lastActiveTimeLocal: Long = 0L
    private var lastIdleTimeLocal: Long = 0L
    private var totalActiveTimeLocal: Long = 0L
    private var totalIdleTimeLocal: Long = 0L
    private var locationJob: Job? = null
    private val gson = Gson()
    private var elementList: ArrayList<String> = ArrayList()
    private var scope: CoroutineScope = createScope()
    private var userDetails: UserDetails? = null
    private var userDataWithImages: UserDetailsWithImages? = null
    private var userId by Delegates.notNull<Long>()
    private var isUserLoggedIn: Boolean = false
    private lateinit var nameLocal: String
    private var adapter: HorizontalImageViewAdapter? = null
    private lateinit var mobileNumberLocal: String
    private var isScopeCreated: Boolean = false
    private var place = ""
    private var road = ""
    private var latlng = ""

    @Inject
    lateinit var sharedPreferences: SharedPreferences
    private fun createScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    private fun formatTime(millis: Long): String {
        val hours = millis / (1000 * 60 * 60)
        val minutes = (millis % (1000 * 60 * 60)) / (1000 * 60)
        val seconds = (millis % (1000 * 60)) / 1000
        return String.format("%02dH:%02dM:%02dS", hours, minutes, seconds)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        userScreenLayoutBinding = UserScreenLayoutBinding.inflate(inflater, container, false)
        userScreenLayoutBinding.punchIn.setOnClickListener {
            showAlertDialog()
        }
        requestBackgroundLocationPermission()
        isUserLoggedIn = sharedPreferences.getBoolean(AppConstants.isUserLoggedIn, false)
        if (isUserLoggedIn) {
            userScreenLayoutBinding.progressBar.visibility = View.VISIBLE
            userId = sharedPreferences.getLong("userId", 0L)
            fetchDetails(userId)
            nameLocal = sharedPreferences.getString(AppConstants.name, "")!!
            mobileNumberLocal = sharedPreferences.getString(AppConstants.mobileNumber, "")!!
            userScreenLayoutBinding.punchOut.visibility = View.VISIBLE
            userScreenLayoutBinding.punchIn.visibility = View.GONE
        } else {
            userScreenLayoutBinding.cameraBtn.visibility = View.GONE
            userScreenLayoutBinding.recyclerViewCard.visibility = View.GONE
            userScreenLayoutBinding.punchOut.visibility = View.GONE
            userScreenLayoutBinding.punchIn.visibility = View.VISIBLE
        }
        userScreenLayoutBinding.punchOut.setOnClickListener {
            userId = 0
            sharedPreferences.edit().putBoolean(AppConstants.isUserLoggedIn, false).apply()
            isScopeCreated = false
            isUserLoggedIn = false
            userScreenLayoutBinding.punchOut.visibility = View.GONE
            userScreenLayoutBinding.punchIn.visibility = View.VISIBLE
            userScreenLayoutBinding.idleTimeValue.text = getString(R.string.idle_time)
            userScreenLayoutBinding.activeTimeValue.text = getString(R.string.idle_time)
            locationJob!!.cancel()
            scope.cancel()
            stopLocationTracking(requireContext())
            userScreenLayoutBinding.recyclerViewCard.visibility = View.GONE
            mobileNumberLocal = ""
            nameLocal = ""
            updateIdleAndActiveTime()
            stopLocation()
            adapter?.clear()
        }
        userScreenLayoutBinding.cameraBtn.setOnClickListener {
            fragmentTransition(CameraFragment())
        }
        return userScreenLayoutBinding.root
    }

    private fun fragmentTransition(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.cameraContainer, fragment)
            .addToBackStack("")
            .commit()
    }

    private fun updateIdleAndActiveTime() {
        scope.launch {
            userViewModel.updateIdleAndActiveTime(
                totalIdleTimeLocal,
                totalActiveTimeLocal,
                lastActiveTimeLocal,
                lastIdleTimeLocal,
                userId
            )
        }
    }

    private fun fetchDetails(userId: Long) {
        scope.launch {
            withContext(Dispatchers.Main) {
                userViewModel.getUserDetails(userId)
                val state = userViewModel.userState.filter { !it.isLoading }
                    .first()
                userDataWithImages = state.userDataImageData ?: throw IllegalStateException("User data is null")
                lastIdleTimeLocal = userDataWithImages!!.userDetails.lastIdleTime!!
                lastActiveTimeLocal = userDataWithImages!!.userDetails.lastActiveTime!!
                totalIdleTimeLocal = userDataWithImages!!.userDetails.idleTime!!
                totalActiveTimeLocal = userDataWithImages!!.userDetails.activeTime!!
                userDetails?.id?.let {
                    sharedPreferences.edit().putLong(AppConstants.userId, it).apply()
                }
                recylerViewInit(userDataWithImages!!.imageDetails as ArrayList)
                getLocation()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        parentFragmentManager.setFragmentResultListener("imageCaptured", this) { _, bundle ->
            fetchDetails(userId)
        }
    }

    fun recylerViewInit(imageDetails: ArrayList<ImageDetails>) {
        adapter = HorizontalImageViewAdapter(requireContext(), imageDetails)
        userScreenLayoutBinding.recyclerViewCard.visibility = View.VISIBLE
        userScreenLayoutBinding.recylerView.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
        userScreenLayoutBinding.recylerView.adapter = adapter
    }

    private fun getLocation() {
        if (!isScopeCreated) {
            scope = createScope()
        }
        sharedPreferences.edit().putBoolean(AppConstants.isUserLoggedIn, true).apply()
        locationJob = scope.launch {
            mapViewModel.locationFlow.collectLatest { locationData ->
                locationData?.let { (isUserMoving, currentTime, location) ->
                    try {
                        if (isUserMoving) {
                            if (lastActiveTimeLocal == 0L) {
                                lastActiveTimeLocal = currentTime
                            }
                            totalActiveTimeLocal += currentTime - lastActiveTimeLocal
                            lastActiveTimeLocal = currentTime
                            lastIdleTimeLocal = 0L
                        } else {
                            if (lastIdleTimeLocal == 0L) {
                                lastIdleTimeLocal = currentTime
                            }
                            totalIdleTimeLocal += currentTime - lastIdleTimeLocal
                            lastIdleTimeLocal = currentTime
                            lastActiveTimeLocal = 0L
                        }
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val addressWithLatLong: MutableList<Address>? =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        elementList.clear()
                        if (addressWithLatLong != null) {
                            place =
                                addressWithLatLong[0].locality + ", " + addressWithLatLong[0].adminArea + ", " + addressWithLatLong[0].countryName
                            road = addressWithLatLong[0].getAddressLine(0)
                            elementList.add(place)
                            elementList.add(road)
                            latlng = "Lat Lng : ${location.latitude}, ${location.longitude}"
                            elementList.add(latlng)
                        }
                        val addressString: String = gson.toJson(elementList)
                        if (userDetails == null) {
                            userDetails = UserDetails(
                                id = userId,
                                name = nameLocal,
                                mobileNumber = mobileNumberLocal,
                                timeStamp = currentTime,
                                lastActiveTime = lastActiveTimeLocal,
                                lastIdleTime = lastIdleTimeLocal,
                                idleTime = totalIdleTimeLocal,
                                activeTime = totalActiveTimeLocal,
                                address = addressString,
                                lantitude = location.latitude.toString(),
                                longitude = location.longitude.toString()
                            )
                        } else {
                            userDetails?.apply {
                                id = userId
                                name = nameLocal
                                mobileNumber = mobileNumberLocal
                                timeStamp = System.currentTimeMillis()
                                lastActiveTime = lastActiveTimeLocal
                                lastIdleTime = lastIdleTimeLocal
                                idleTime = totalIdleTimeLocal
                                activeTime = totalActiveTimeLocal
                                address = addressString
                                lantitude = location.latitude.toString()
                                longitude = location.longitude.toString()
                                timeStamp = currentTime
                            }
                        }
                        userViewModel.udpateUserDetails(userDetails!!)
                        withContext(Dispatchers.Main) {
                            userScreenLayoutBinding.progressBar.visibility = View.GONE
                            userScreenLayoutBinding.punchOut.visibility = View.VISIBLE
                            userScreenLayoutBinding.punchIn.visibility = View.GONE
                            userScreenLayoutBinding.cameraBtn.visibility = View.VISIBLE
                            userScreenLayoutBinding.idleTimeValue.text =
                                formatTime(totalIdleTimeLocal)
                            userScreenLayoutBinding.activeTimeValue.text =
                                formatTime(totalActiveTimeLocal)
                        }
                    } catch (e: Exception) {
                        e.message
                    }
                }
            }
        }
    }

    private fun startLocationTracking(context: Context) {
        val data = Data.Builder()
        data.putLong(AppConstants.userId, sharedPreferences.getLong(AppConstants.userId, 0L))
        data.putString(AppConstants.name, sharedPreferences.getString(AppConstants.name, ""))
        data.putString(
            AppConstants.mobileNumber,
            sharedPreferences.getString(AppConstants.mobileNumber, "")
        )
        data.putLong(AppConstants.lastIdleTimeLocal, lastIdleTimeLocal)
        data.putLong(AppConstants.lastActiveTimeLocal, lastActiveTimeLocal)
        data.putLong(AppConstants.totalIdleTimeLocal, totalIdleTimeLocal)
        data.putLong(AppConstants.totalActiveTimeLocalr, totalActiveTimeLocal)
        val workRequest = OneTimeWorkRequestBuilder<LocationWorker>().setInputData(data.build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "LocationTracking",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun stopLocationTracking(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork("LocationTracking")
    }

    private fun requestBackgroundLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                1
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationTracking(requireContext())
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.background_location_permission_denied),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }


    private fun showAlertDialog() {
        val linearLayout = LinearLayout(requireActivity())
        val contextThemeWrapper = ContextThemeWrapper(requireActivity(), R.style.MyDenseOutlined)
        val widthInputLayout = TextInputLayout(contextThemeWrapper)
        val nameEditText = TextInputEditText(contextThemeWrapper)
        nameEditText.apply {
            inputType = InputType.TYPE_TEXT_VARIATION_PERSON_NAME
        }
        widthInputLayout.apply {
            hint = context.getString(R.string.name)
            addView(nameEditText)
        }

        val heightInputLayout = TextInputLayout(contextThemeWrapper)
        val mobileNumberEditText = TextInputEditText(contextThemeWrapper)
        mobileNumberEditText.apply {
            inputType = InputType.TYPE_CLASS_PHONE
        }
        heightInputLayout.apply {
            hint = context.getString(R.string.mobile_number)
            addView(mobileNumberEditText)
        }
        linearLayout.apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 16, 16, 16)
            addView(widthInputLayout)
            addView(heightInputLayout)
        }
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setView(linearLayout)
            .setCancelable(false)
            .setTitle(getString(R.string.user_details))
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton(getString(R.string.ok), null)
            .create()
        alertDialog.show()
        alertDialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE)
            .setOnClickListener {
                alertDialog.dismiss()
            }
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val validateName = validateName(mobileNumberEditText)
            val validateMobile = validateMobile(mobileNumberEditText)
            userScreenLayoutBinding.progressBar.visibility = View.VISIBLE
            if (validateMobile && validateName) {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        if (!mobilNumberCheck(mobileNumberEditText)) {
                            val userState = UserDetails(
                                name = nameEditText.text.toString(),
                                mobileNumber = mobileNumberEditText.text.toString(),
                                timeStamp = System.currentTimeMillis(),
                                lastActiveTime = 0L,
                                lastIdleTime = 0L,
                                idleTime = 0L,
                                activeTime = 0L
                            )
                            withContext(Dispatchers.Main) {
                                userViewModel.createUser(userState)
                                val state = userViewModel.createUserstate.filter { !it.isLoading }
                                    .first()
                                if (userState.id != null) {
                                    userId = state.id!!
                                }
                                sharedPreferences.edit().putLong(AppConstants.userId, userId)
                                    .apply()
                                sharedPreferences.edit()
                                    .putString(AppConstants.name, nameEditText.text.toString())
                                    .apply()
                                sharedPreferences.edit().putString(
                                    AppConstants.mobileNumber,
                                    mobileNumberEditText.text.toString()
                                ).apply()
                                nameLocal = nameEditText.text.toString()
                                mobileNumberLocal = mobileNumberEditText.text.toString()
                                lastActiveTimeLocal = 0L
                                totalActiveTimeLocal = 0L
                                lastIdleTimeLocal = 0L
                                totalIdleTimeLocal = 0L
                                alertDialog.dismiss()
                                sharedPreferences.edit()
                                    .putBoolean(AppConstants.isUserLoggedIn, true).apply()
                                getLocation()
                            }
                        } else {
                            userViewModel.getUserDataWithMobileNumber(mobileNumberEditText.text.toString())
                            val state = userViewModel.userStateMobileNumber.filter { !it.isLoading }
                                .first()
                            if (state.userDetails?.id != null) {
                                userId = state.userDetails.id
                                userDetails = state.userDetails
                                sharedPreferences.edit()
                                    .putLong(AppConstants.userId, userDetails?.id!!)
                                    .apply()
                                sharedPreferences.edit()
                                    .putString(AppConstants.name, userDetails?.name)
                                    .apply()
                                sharedPreferences.edit()
                                    .putString(AppConstants.mobileNumber, userDetails?.mobileNumber)
                                    .apply()
                                alertDialog.dismiss()
                                nameLocal = nameEditText.text.toString()
                                mobileNumberLocal = mobileNumberEditText.text.toString()
                                lastActiveTimeLocal = userDetails?.lastActiveTime!!
                                lastIdleTimeLocal = userDetails?.lastIdleTime!!
                                totalIdleTimeLocal = userDetails?.idleTime!!
                                totalActiveTimeLocal = userDetails?.activeTime!!
                                getLocation()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    AppConstants.invalidCredential,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private suspend fun mobilNumberCheck(mobileEdittext: TextInputEditText): Boolean {
        userViewModel.getMobileNumbers()
        val state = userViewModel.mobileNumberState
            .filter { !it.isLoading }
            .first()
        return state.mobileNumbers?.contains(mobileEdittext.text.toString()) == true
    }

    private fun validateName(
        nameEditText: TextInputEditText
    ): Boolean {
        val name = nameEditText.text.toString().trim()
        return name.isNotEmpty()
    }

    private fun validateMobile(
        mobileEdittext: TextInputEditText
    ): Boolean {
        val name = mobileEdittext.text.toString().trim()
        return !(name.isEmpty() && mobileEdittext.text!!.length <= 10)
    }

    override fun onResume() {
        super.onResume()
        if (isUserLoggedIn) {
            stopLocationTracking(requireContext())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (locationJob != null) {
            locationJob!!.cancel()
            scope.cancel()
        }
    }

    override fun onStop() {
        super.onStop()
        isUserLoggedIn = sharedPreferences.getBoolean(AppConstants.isUserLoggedIn, false)
        if (isUserLoggedIn) {
            startLocationTracking(requireContext())
        } else {
            stopLocationTracking(requireContext())
        }
    }

    private fun stopLocation() {
        locationJob!!.cancel()
    }
}