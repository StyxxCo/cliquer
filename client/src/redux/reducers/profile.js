function profile(state = {}, action) {
    switch(action.type) {
        case 'GET_PROFILE_HAS_ERROR':
            return Object.assign({}, state, {
                getHasError: action.hasError,
            })
        case 'GET_PROFILE_DATA_SUCCESS':
            return Object.assign({}, state, {
                getData: action.data,
            })
        case 'GET_PROFILE_IS_LOADING':
            return Object.assign({}, state, {
                getIsLoading: action.isLoading,
            })
        case 'DELETE_PROFILE_HAS_ERROR':
            return Object.assign({}, state, {
                deleteHasError: action.hasError,
            })
        case 'DELETE_PROFILE_DATA_SUCCESS':
            return Object.assign({}, state, {
                deleteData: action.data,
            })
        case 'DELETE_PROFILE_IS_LOADING':
            return Object.assign({}, state, {
                deleteIsLoading: action.isLoading,
            })
        case 'SET_SETTINGS_HAS_ERROR':
            return Object.assign({}, state, {
                setSettingsHasError: action.hasError,
            })
        case 'SET_SETTINGS_DATA_SUCCESS':
            return Object.assign({}, state, {
                setSettingsData: action.data,
            })
        case 'SET_SETTINGS_IS_LOADING':
            return Object.assign({}, state, {
                setSettingsIsLoading: action.isLoading,
            })
        case 'SET_LOCATION_HAS_ERROR':
            return Object.assign({}, state, {
                setLocationHasError: action.hasError,
            })
        case 'SET_LOCATION_DATA_SUCCESS':
            return Object.assign({}, state, {
                setLocationData: action.data,
            })
        case 'SET_LOCATION_IS_LOADING':
            return Object.assign({}, state, {
                setLocationIsLoading: action.isLoading,
            })
        case 'UPLOAD_FILE_HAS_ERROR':
            return Object.assign({}, state, {
                uploadFileHasError: action.hasError,
            })
        case 'UPLOAD_FILE_DATA_SUCCESS':
            return Object.assign({}, state, {
                uploadFileData: action.data,
            })
        case 'UPLOAD_FILE_IS_LOADING':
            return Object.assign({}, state, {
                uploadFileIsLoading: action.isLoading,
            })
        case 'CLEAR_PROFILE':
            return Object.assign({}, state, {
                getData: null,
            })
        default:
            return state
    }
}

export default profile