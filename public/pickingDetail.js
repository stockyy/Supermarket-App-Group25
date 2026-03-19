const modal = document.getElementById('substitution-modal');
const modalOriginalItem = document.getElementById('modal-original-item');
const confirmSubstituteBtn = document.getElementById('confirm-substitute-btn');
const cancelSubstituteBtn = document.getElementById('cancel-substitute-btn');
const selectedSubstituteDiv = document.getElementById('selected-substitute');
const selectedSubstituteName = document.getElementById('selected-substitute-name');
const substituteSearch = document.getElementById('substitute-search');
const substituteResults = document.getElementById('substitute-results');

let currentItemId = null;
let selectedProductId = null;
