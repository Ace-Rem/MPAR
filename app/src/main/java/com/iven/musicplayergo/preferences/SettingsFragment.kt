package com.acerem.musicplayerar.preferences
import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.acerem.musicplayerar.GoPreferences
import com.acerem.musicplayerar.R
import com.acerem.musicplayerar.databinding.FragmentSettingsBinding
import com.acerem.musicplayerar.ui.UIControlInterface


/**
 * A simple [Fragment] subclass.
 * Use the [SettingsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingsFragment : Fragment() {

    private var _fragmentSettingsBinding: FragmentSettingsBinding? = null

    private lateinit var mUIControlInterface: UIControlInterface

    private var mPreferencesFragment: PreferencesFragment? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        try {
            mUIControlInterface = activity as UIControlInterface
        } catch (e: ClassCastException) {
            e.printStackTrace()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _fragmentSettingsBinding = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _fragmentSettingsBinding = FragmentSettingsBinding.inflate(inflater, container, false)
        return _fragmentSettingsBinding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _fragmentSettingsBinding?.searchToolbar?.run {
            inflateMenu(R.menu.menu_settings)
            setNavigationOnClickListener {
                mUIControlInterface.onCloseActivity()
            }
            setOnMenuItemClickListener {
                if (it.itemId == R.id.locale_switcher) openLocaleSwitcher()
                return@setOnMenuItemClickListener true
            }
        }

        mPreferencesFragment = PreferencesFragment.newInstance()
        mPreferencesFragment?.let { fm ->
            childFragmentManager.commit {
                replace(R.id.fragment_layout, fm)
            }
        }
    }

    fun getPreferencesFragment() = mPreferencesFragment

    @SuppressLint("SuspiciousIndentation")
    private fun openLocaleSwitcher() {
        val locales = ContextUtils.getLocalesList(resources)
        val dialog: MaterialAlertDialogBuilder = MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.locale_pref_title).setItems(locales.values.toTypedArray()) { _, which ->
            // Respond to item chosen
            val newLocale = locales.keys.elementAt(which)
            if (GoPreferences.getPrefsInstance().locale != newLocale) {
                GoPreferences.getPrefsInstance().locale = locales.keys.elementAt(which)
                mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
            }
        }.setNegativeButton(R.string.cancel, null)

        if (GoPreferences.getPrefsInstance().locale != null) {
            dialog.setNeutralButton(R.string.sorting_pref_default) { _, _ ->
                GoPreferences.getPrefsInstance().locale = null
                mUIControlInterface.onAppearanceChanged(isThemeChanged = false)
            }
        }
        dialog.show()
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @return A new instance of fragment SettingsFragment.
         */
        @JvmStatic
        fun newInstance() = SettingsFragment()
    }
}
