let commonURL = "";
axios.defaults.baseURL = commonURL;
axios.defaults.timeout = 10000;
let token = sessionStorage.getItem("token");
axios.interceptors.request.use(
  config => {
    const currentToken = sessionStorage.getItem("token");
    if (currentToken) {
      config.headers['authorization'] = currentToken;
      token = currentToken;
    }
    return config
  },
  error => {
    console.log(error)
    return Promise.reject(error)
  }
)
axios.interceptors.response.use(function (response) {
  if (!response.data.success) {
    return Promise.reject(response.data.errorMsg)
  }
  return response.data;
}, function (error) {
  console.log(error)
  const status = error.response && error.response.status;
  if (status === 401) {
    sessionStorage.removeItem("token");
    token = null;
    location.href = "/login.html";
    return Promise.reject("Please log in first");
  }
  if (status) {
    return Promise.reject("Server error");
  }
  return Promise.reject("Network error, please try again");
});
axios.defaults.paramsSerializer = function(params) {
  let p = "";
  Object.keys(params).forEach(k => {
    if(params[k]){
      p = p + "&" + k + "=" + params[k]
    }
  })
  return p;
}
const util = {
  commonURL,
  getUrlParam(name) {
    let reg = new RegExp("(^|&)" + name + "=([^&]*)(&|$)", "i");
    let r = window.location.search.substr(1).match(reg);
    if (r != null) {
      return decodeURI(r[2]);
    }
    return "";
  },
  formatPrice(val) {
    if (typeof val === 'string') {
      if (isNaN(val)) {
        return null;
      }
      // Convert price to integer cents
      const index = val.lastIndexOf(".");
      let p = "";
      if (index < 0) {
        p = val + "00";
      } else if (index === p.length - 2) {
        p = val.replace("\.", "") + "0";
      } else {
        p = val.replace("\.", "")
      }
      return parseInt(p);
    } else if (typeof val === 'number') {
      if (!val) {
        return null;
      }
      const s = val + '';
      if (s.length === 0) {
        return "0.00";
      }
      if (s.length === 1) {
        return "0.0" + val;
      }
      if (s.length === 2) {
        return "0." + val;
      }
      const i = s.indexOf(".");
      if (i < 0) {
        return s.substring(0, s.length - 2) + "." + s.substring(s.length - 2)
      }
      const num = s.substring(0, i) + s.substring(i + 1);
      if (i === 1) {
        return "0.0" + num;
      }
      if (i === 2) {
        return "0." + num;
      }
      if (i > 2) {
        return num.substring(0, i - 2) + "." + num.substring(i - 2)
      }
    }
  }
}
